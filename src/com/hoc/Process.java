package com.hoc;

import com.github.pjfanning.xlsx.StreamingReader;
import com.hoc.model.*;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;


public class Process {
  private static final int EXAMINER_FIRST_ROW = 1;
  private static final int EXAMINER_STT_COLUMN = 0;
  private static final int EXAMINER_ID_COLUMN = 1;
  private static final int EXAMINER_NAME_COLUMN = 2;
  private static final int EXAMINER_BIRTHDAY_COLUMN = 3;
  private static final int EXAMINER_UNIT_COLUMN = 4;

  private static final int ROOM_NAME_COLUMN = 1;
  private static final int ROOM_FIRST_ROW = 1;

  private Process() {
  }

  public static final CheckedFunction<Socket, byte[]> readBytes = Process::readBytes;
  public static final CheckedFunction<byte[], Pair<List<Examiner>, List<Room>>> readExcel = Process::readExcel;
  public static final CheckedFunction<Pair<List<Examiner>, List<Room>>, List<Result>> distribution = Process::distribution;
  public static final CheckedFunction<List<Result>, Buffer> writeExcelFile = Process::writeExcelFile;

  private static byte[] readBytes(Socket socket) throws IOException, ClassNotFoundException {
    System.out.println("Start read");
    final ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
    final byte[] bytes = ((Buffer) inputStream.readObject()).bytes;
    System.out.println("Read done: " + bytes.length);
    return bytes;
  }

  private static Pair<List<Examiner>, List<Room>> readExcel(byte[] bytes) throws IOException {
    final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    System.out.println(is);
    final Workbook workbook = StreamingReader.builder()
      .rowCacheSize(2000)
      .bufferSize(1024 * 8)
      .open(is);

    List<Examiner> examiners = new ArrayList<>();
    List<Room> rooms = new ArrayList<>();

    workbook.getSheetAt(0).forEach(row -> {
      if (row.getRowNum() >= EXAMINER_FIRST_ROW) {
        final String id = row.getCell(EXAMINER_ID_COLUMN).getStringCellValue();
        final String name = row.getCell(EXAMINER_NAME_COLUMN).getStringCellValue();
        final String birthday = row.getCell(EXAMINER_BIRTHDAY_COLUMN).getStringCellValue();
        final String unit = row.getCell(EXAMINER_UNIT_COLUMN).getStringCellValue();
        examiners.add(new Examiner(id, name, birthday, unit));
      }
    });

    workbook.getSheetAt(1).forEach(row -> {
      if (row.getRowNum() >= ROOM_FIRST_ROW) {
        final String name = row.getCell(ROOM_NAME_COLUMN).getStringCellValue();
        rooms.add(new Room(name));
      }
    });

    workbook.close();
    System.out.println("Read excel done: " + examiners.size() + ".." + rooms.size());

    return new Pair<>(Collections.unmodifiableList(examiners), Collections.unmodifiableList(rooms));
  }

  private static List<Result> distribution(Pair<List<Examiner>, List<Room>> pair) {
    final List<Examiner> examiners = new ArrayList<>(pair.first);
    final List<Room> rooms = new ArrayList<>(pair.second);

    final int minNumberExaminers = rooms.size() * 2;
    final int remainExaminers = examiners.size() - minNumberExaminers;

    if (remainExaminers < 0) {
      throw new IllegalStateException("Not enough examiner");
    }

    final Map<String, Result> map = new HashMap<>();

    final int roomPerExaminer = (int) Math.ceil(rooms.size() * 1.0 / remainExaminers);
    final List<List<Room>> partition = ListUtils.partition(rooms, roomPerExaminer);

    int offset = examiners.size() / remainExaminers;
    System.out.println("Offset: " + offset);
    System.out.println("roomPerExaminer: " + roomPerExaminer);

    int outsideRoomsIndex = offset - 1;
    int count = 0;
    for (List<Room> roomList : partition) {
      if (outsideRoomsIndex < examiners.size() && count < rooms.size()) {
        final Examiner examiner = examiners.get(outsideRoomsIndex);
        map.put(
          examiner.id,
          new Result2(
            roomList,
            examiner
          )
        );
        outsideRoomsIndex += offset;
        count += roomList.size();
      }
    }

    System.out.println("UsedIds: " + map.size());
    System.out.println("count: " + count);
    System.out.println("partition: " + partition.stream().map(i -> i.size() + "").collect(Collectors.joining(",")));
    System.out.println("partition: " + partition.size());

    final List<Examiner> collect = examiners.stream()
      .filter(examiner -> !map.containsKey(examiner.id))
      .limit(minNumberExaminers)
      .collect(Collectors.toList());
    for (int i = 0, roomIndex = 0; i < collect.size() && roomIndex < rooms.size(); i += 2) {
      final Room room = rooms.get(roomIndex++);
      final Examiner examiner1 = collect.get(i);
      final Examiner examiner2 = collect.get(i + 1);
      map.put(
        examiner1.id,
        new Result1(
          examiner1,
          room,
          1
        )
      );
      map.put(
        examiner2.id,
        new Result1(
          examiner2,
          room,
          2
        )
      );
      System.out.println(roomIndex);
    }

    final List<Result> results = pair.first.stream()
      .map(examiner -> map.get(examiner.id))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    System.out.println(results.stream().filter(i -> i instanceof Result1).count());
    System.out.println(results.stream().filter(i -> i instanceof Result2).count());
    return results;
  }

  private static Buffer writeExcelFile(List<Result> results) throws IOException {
    final SXSSFWorkbook workbook = new SXSSFWorkbook();

    final SXSSFSheet sheet1 = workbook.createSheet("Tổng hợp");
    writeSheet(results, sheet1);

    int index = 0;
    for (List<Result> chunk : ListUtils.partition(results, 20)) {
      writeSheet(
        chunk,
        workbook.createSheet("Sheet " + ++index)
      );
    }

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    return new Buffer(baos.toByteArray());
  }

  private static void writeSheet(List<Result> results, SXSSFSheet sheet1) {
    final SXSSFRow row1 = sheet1.createRow(0);
    row1.createCell(0).setCellValue("Cộng hòa Xã hội Chủ nghĩa Việt Nam");
    sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
    center(row1.getCell(0));

    final SXSSFRow row2 = sheet1.createRow(1);
    row2.createCell(0).setCellValue("Độc lập - Tự do - Hạnh phúc");
    sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
    center(row2.getCell(0));

    final SXSSFRow row3 = sheet1.createRow(2);
    row3.createCell(0).setCellValue("DANH SÁCH PHÂN CÔNG COI THI");
    sheet1.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));
    center(row3.getCell(0));

    int rowNumber = 4;
    Row header = sheet1.createRow(rowNumber++);

    int headerColumnNumber = 0;
    for (String str : Arrays.asList("STT", "Số thẻ", "Họ và tên", "Ngày sinh", "Đơn vị công tác", "Phòng thi", "Chức vụ", "Ghi chú")) {
      Cell cell = header.createCell(headerColumnNumber++);
      cell.setCellValue(str);
    }

    int stt = 1;
    for (Result result : results) {
      if (result instanceof Result1) {
        final Result1 result1 = (Result1) result;
        final Examiner examiner = result1.examiner;

        final Row row = sheet1.createRow(rowNumber++);
        row.createCell(0).setCellValue(stt++);
        row.createCell(1).setCellValue(examiner.id);
        row.createCell(2).setCellValue(examiner.name);
        row.createCell(3).setCellValue(examiner.birthday);
        row.createCell(4).setCellValue(examiner.unit);
        row.createCell(5).setCellValue(result1.room.name);
        row.createCell(6).setCellValue("Giám thị " + result1.role);
        row.createCell(7).setCellValue("");

      } else if (result instanceof Result2) {
        final Result2 result2 = (Result2) result;
        final Examiner examiner = result2.examiner;
        final List<Room> rooms = result2.rooms;

        final Row row = sheet1.createRow(rowNumber++);
        row.createCell(0).setCellValue(stt++);
        row.createCell(1).setCellValue(examiner.id);
        row.createCell(2).setCellValue(examiner.name);
        row.createCell(3).setCellValue(examiner.birthday);
        row.createCell(4).setCellValue(examiner.unit);
        row.createCell(5).setCellValue("");
        row.createCell(6).setCellValue("Giám thị hành lang");
        row.createCell(7).setCellValue("Từ " + rooms.get(0).name + "-" + rooms.get(rooms.size() - 1).name);
      }
    }
  }

  private static void center(Cell startCell) {
    CellStyle style = startCell.getSheet().getWorkbook().createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    startCell.setCellStyle(style);
  }
}
