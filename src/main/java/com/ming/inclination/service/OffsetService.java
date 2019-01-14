package com.ming.inclination.service;

import com.ming.inclination.dao.TblFilterOffsetDao;
import com.ming.inclination.dao.TblOriginOffsetDao;
import com.ming.inclination.entity.*;
import com.ming.inclination.util.Inclinator;
import com.ming.inclination.util.MyUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unchecked")
@Service
public class OffsetService {

    @Autowired
    private DataService dataService;

    @Autowired
    private FilterService filterService;

    @Autowired
    TblOriginOffsetDao tblOriginOffsetDao;

    @Autowired
    TblFilterOffsetDao tblFilterOffsetDao;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${bridgeId}")
    /**桥梁id**/
    private String bridgeId;
    @Value("${canIds}")
    private String canIds;//倾角仪ID字符串
    @Value("${canDistance}")
    private String canDistance;//倾角仪距离字符串
    @Value("${canNumber}")
    private int canNumber;//倾角仪数量
    @Value("${folderPath}")
    private String folderPath;//目录位置

    private final static Logger LOGGER = LogManager.getLogger(OffsetService.class);
    private final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final DateFormat sdfm = new SimpleDateFormat("yyyy-MM-dd HH:mm");


    /**
     * 读取CSV文件
     *
     * @return
     */
    @Scheduled(fixedDelay = 30*1000)
    @Transactional(rollbackFor = Exception.class)
    public void getOriData() {
        LOGGER.info("getOriData Activated");
        File folder = new File(folderPath);
        File file = null;

        try {
            if (folder.exists()) {
                File[] fileList = folder.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].length() != 0L) {
                        file = fileList[i];
                        break;
                    }
                    file = fileList[i];
                    file.delete();
                }
            }

            if (file != null) {

                Iterable<CSVRecord> records;
                try (Reader in = new InputStreamReader(new FileInputStream(file), "GBK")) {
                    records = CSVFormat.EXCEL
                            .withHeader("序号", "系统时间", "时间标识", "CAN通道", "传输方向", "ID号", "帧类型", "帧格式", "长度", "数据")
                            .parse(in)
                            .getRecords();
                }

                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                FileLock fileLock = channel.tryLock();

                if (records != null && ((List<CSVRecord>) records).size() > 0) {
                    //获取数据
                    analysisRecord(records);
                }

                clearFile(file);
                fileLock.release();
                raf.close();
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * 从CSV文件获取数据
     *
     * @param records
     */
    public void analysisRecord(Iterable<CSVRecord> records) {
        StringBuffer date = new StringBuffer(47);
        //当日年月日
        date.append(MyUtils.getToday());

        LinkedHashMap<String, ArrayList<String>> timeMap = new LinkedHashMap<>();
        int index = 0;

        for (CSVRecord record : records) {
            if (index == 0) {
                index++;
            } else {
                String canId = record.get("ID号");
                String time = record.get("系统时间").substring(2, 14);
                String data = record.get("数据").substring(3, 26).replace(" ", "");
                //TODO
                if (!"0x0590".equals(canId)) {
                    String actualTime = date.append(time).substring(0,18);
                    if (!timeMap.containsKey(actualTime)) {
                        timeMap.put(actualTime, new ArrayList<>());
                    }
                    //获取x个传感器数据
                    timeMap.get(actualTime).add(date.append("@").append(canId).append("@").append(data).toString());
                    date.delete(0, date.length());
                    date.append(MyUtils.getToday());
                }
            }
        }
        //组装数据
        buildSecMap(timeMap);
    }

    /**
     * 以分钟为key 按id分组整理顺序
     *
     * @param timeMap
     */
    private void buildSecMap(LinkedHashMap<String, ArrayList<String>> timeMap) {
        List<String> dataList = new ArrayList<>();
        for (String key : timeMap.keySet()) {
            LinkedHashMap<String, ArrayList<String>> secMap = new LinkedHashMap<>();
            ArrayList<String> tempList = timeMap.get(key);

            for (int i = 0; i < tempList.size(); i++) {
                String canId = StringUtils.split(tempList.get(i),"@")[1];
                if (!secMap.containsKey(canId)) {
                    secMap.put(canId, new ArrayList<>());
                }
                secMap.get(canId).add(tempList.get(i));
            }
            buildDataTeam(secMap, dataList);
            secMap.clear();
        }
        List<TblOriginOffset> originOffsetList = toOriOffset(dataList);
        dataService.insertAllOrigin(originOffsetList);
    }

    /**
     * 按顺序组装数据
     *
     * @param secMap
     */
    private void buildDataTeam(LinkedHashMap<String, ArrayList<String>> secMap, List<String> dataList) {
        List<String> tempList = new ArrayList<>(canNumber);
        int length = 999;
        //获取最小长度
        for (String key : secMap.keySet()) {
            if (secMap.get(key).size() < length) {
                length = secMap.get(key).size();
            }
        }
        //组装为一组数据
        for (int i = 0; i < length; i++) {
            for (String key : secMap.keySet()) {
                tempList.add(secMap.get(key).get(i));
            }
            if (tempList.size() == canNumber) {
                dataList.addAll(tempList);
                tempList.clear();
            }
        }
    }

    @Async("taskExecutor")
    @Scheduled(initialDelay = 60*1000, fixedRate = 60*1000)
    @Transactional(rollbackFor = Exception.class)
    public void doFilter() {
        LOGGER.info("doFilter Activated");
        long currentTime = System.currentTimeMillis();
        String headTime = sdf.format(new Date(currentTime - 2 * 60 * 1000 - 1));
        String endTime = sdf.format(new Date(currentTime + 1));
        String headTime2 = sdf.format(new Date(currentTime - 90 * 1000 - 1));
        String endTime2 = sdf.format(new Date(currentTime - 30 * 1000 + 1));

        List<TblOriginOffset> originOffsetList = tblOriginOffsetDao.findDataByTimeRange(headTime, endTime);

        if (originOffsetList.size() > 0) {
            try {
                LOGGER.warn("待过滤数据：" + originOffsetList.size() + " 条");
                List<TblFilterOffset> filteredList = new ArrayList<>();
                String[] canIdArr = StringUtils.split(canIds,",");
                filterService.throughFilter(originOffsetList, filteredList, canNumber, canIdArr);
                dataService.insertAllFilter(filteredList);
                LOGGER.warn("储存过滤数据共 " + filteredList.size() + " 条");

                doOffset(headTime2, endTime2);
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
    }

    private void doOffset(String headTime, String endTime) {
        System.out.println("开始挠度换算");
        List<TblFilterOffset> filterTempList = new ArrayList<>(canNumber);
        HashMap<String, Double> canIdRadianMap = new HashMap<>(canNumber);
        List<TblMeasurePointOffset> measurePointList = getMeasurePoints();
        List<TblFilterOffset> filteredList = tblFilterOffsetDao.findDataByTimeRange(headTime, endTime);
        List<TblDataOffset> offsetList = new ArrayList<>();

        for (int i = 0; i < filteredList.size(); i++) {
            filterTempList.add(filteredList.get(i));
            if (filterTempList.size() == canNumber) {
                //存放倾角值
                filterTempList.forEach(item -> canIdRadianMap.put(item.getCanId(), parseCoordinateRadian(item.getValueX(), item.getValueY()).getY()));
                if (canIdRadianMap.size() == measurePointList.size()) {
                    addEightPointOffset(canIdRadianMap, measurePointList, filteredList.get(i).getDataTime(), bridgeId, offsetList);
                }
                canIdRadianMap.clear();
                filterTempList.clear();
            }
        }

        dataService.insertAllData(offsetList);
        LOGGER.warn("储存挠度换算数据共 " + filteredList.size() + " 条");
    }

    /**
     * 挠度计算
     *
     * @param canIdRadianMap
     * @param measurePointList
     * @param acTime
     * @param bridgeId
     */
    private void addEightPointOffset(HashMap<String, Double> canIdRadianMap, List<TblMeasurePointOffset> measurePointList, String acTime, String bridgeId, List<TblDataOffset> OffsetList) {
        List<Double> positionList = new ArrayList<>();
        List<Double> radianList = new ArrayList<>();
        for (TblMeasurePointOffset measurePoint : measurePointList) {
            positionList.add(measurePoint.getPosition());
            radianList.add(canIdRadianMap.get(measurePoint.getCanId()));
        }
        //计算挠度，同时保存计算结果
        List<TblDataOffset> offsetList = Inclinator.getEightPointDeflection(positionList, radianList);
        //只保留四分点和跨中
        List<TblDataOffset> storeOffsetList = new ArrayList<>();
        Collections.addAll(storeOffsetList, offsetList.get(2), offsetList.get(4), offsetList.get(6));
        for (int i = 0; i < storeOffsetList.size(); i++) {
            storeOffsetList.get(i).setId(MyUtils.generateUUID());
            storeOffsetList.get(i).setBridgeId(bridgeId);
            storeOffsetList.get(i).setMeasurePoint(String.format("WY-%02d-Q-01", i + 1));
            storeOffsetList.get(i).setAcTime(acTime);
            try {
                storeOffsetList.get(i).setMinZone(sdfm.format(sdf.parse(acTime)));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            storeOffsetList.get(i).setUploaded("0");
        }
        //保存计算结果
        OffsetList.addAll(storeOffsetList);
    }

    /**
     * 以秒为单位取出位移值并计算最大值、最小值、平均值
     */
    @Scheduled(initialDelay = 120*1000,fixedRate = 120 * 1000)
    @Transactional(rollbackFor = Exception.class)
    public void getOffsetResult() {
        LOGGER.info("getOffsetResult Activated");

        String sql = "SELECT tdo.measure_point, " +
                "tdo.min_zone as minZone," +
                "MIN(tdo.`offset`) as minOffset," +
                "MAX(tdo.`offset`) as maxOffset," +
                "AVG(tdo.`offset`) as avgOffset " +
                "FROM tbl_data_offset tdo " +
                "WHERE tdo.measure_point = 'WY-02-Q-01' " +
                "AND tdo.uploaded='0' " +
                "GROUP BY tdo.min_zone " +
                "ORDER BY tdo.min_zone ";
        List<DataOffsetVo> voList = namedParameterJdbcTemplate.query(sql, new BeanPropertyRowMapper<>(DataOffsetVo.class));
        if (voList.size() > 0) {
            LOGGER.warn("聚合查询结果：" + voList.size() + " 条");
            try {
                dataService.insertAll(transformOrcList(voList));
                Map<String, Object> paramMap = new HashMap<>();
                List<String> minZones = new ArrayList<>();
                sql = "UPDATE tbl_data_offset tdo SET tdo.uploaded='1' WHERE tdo.min_zone in (:minZones)";
                for (int i = 0; i < voList.size(); i++) {
                    minZones.add(voList.get(i).getMinZone());
                }
                paramMap.put("minZones", minZones);
                namedParameterJdbcTemplate.update(sql, paramMap);
            } catch (Exception e) {
                LOGGER.error(e);

            }
        }
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void deleteOriData() {
        String beforeDay = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        Map<String, Object> paramMap = new HashMap<>();
        String sql = "delete from tbl_origin_offset where data_time<=:beforeDay";
        paramMap.put("beforeDay", beforeDay);
        namedParameterJdbcTemplate.update(sql, paramMap);
        LOGGER.warn("已删除" + sdf.format(beforeDay) + "之前的原始数据");
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void deleteFilterData() {
        String beforeDay = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        Map<String, Object> paramMap = new HashMap<>();
        String sql = "delete from tbl_filter_offset where data_time<=:beforeDay";
        paramMap.put("beforeDay", beforeDay);
        namedParameterJdbcTemplate.update(sql, paramMap);
        LOGGER.warn("已删除" + sdf.format(beforeDay) + "之前的过滤数据");
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void deleteOffsetData() {
        String beforeDay = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        Map<String, Object> paramMap = new HashMap<>();
        String sql = "delete from tbl_data_offset where data_time<=:beforeDay";
        paramMap.put("beforeDay", beforeDay);
        namedParameterJdbcTemplate.update(sql, paramMap);
        LOGGER.warn("已删除" + sdf.format(beforeDay) + "之前的位移数据");
    }

    private List<TblMeasurePointOffset> getMeasurePoints() {
        List<TblMeasurePointOffset> measurePointList = new ArrayList<>(canNumber);
        String[] canIdArr = StringUtils.split(canIds,",");
        String[] canDisArr = StringUtils.split(canDistance,",");
        for (int i = 0; i < canIdArr.length; i++) {
            TblMeasurePointOffset pointOffset = new TblMeasurePointOffset();
            pointOffset.setCanId(canIdArr[i]);
            pointOffset.setPosition(Double.valueOf(canDisArr[i]));
            measurePointList.add(pointOffset);
        }
        return measurePointList;
    }

    /**
     * 数据字符串转换为数据对象
     *
     * @param dataList
     * @return
     */
    private List<TblOriginOffset> toOriOffset(List<String> dataList) {

        if (!dataList.isEmpty()) {
            List<TblOriginOffset> originOffsetList = new ArrayList<>(dataList.size());
            for (int i = 0; i < dataList.size(); i++) {
                //解析倾角仪数据
                TblOriginOffset offset = new TblOriginOffset();
                String[] param = StringUtils.split(dataList.get(i),"@");
                String time = param[0];
                offset.setCanId(param[1]);
                offset.setData(param[2]);

                offset.setId(MyUtils.generateUUID());
                String xStr = StringUtils.substring(offset.getData(),0, 8);
                String yStr = StringUtils.substring(offset.getData(),8);
                offset.setValueX(MyUtils.decimalParse(xStr));
                offset.setValueY(MyUtils.decimalParse(yStr));
                offset.setDataTime(time);
                originOffsetList.add(offset);
            }
            return originOffsetList;
        } else {
            return null;
        }

    }

    /**
     * 转化为ORACLE数据库存储对象
     *
     * @param aggVoList
     * @return
     */
    private List<TblDataOffsetToOrcl> transformOrcList(List<DataOffsetVo> aggVoList) throws ParseException {
        List<TblDataOffsetToOrcl> dataOffsetList = new ArrayList<>();
        int size = aggVoList.size();

        for (int i = 0; i < size; i++) {
            DataOffsetVo dataOffsetVo = aggVoList.get(i);
            for (int j = 0; j < 3; j++) {
                TblDataOffsetToOrcl orcVo = new TblDataOffsetToOrcl();
                orcVo.setId(MyUtils.generateUUID());
                orcVo.setBridgeId(bridgeId);
                orcVo.setMeasurePoint(dataOffsetVo.getMeasurePoint());
                if (j == 0) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getMinOffset()));
                }
                if (j == 1) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getMaxOffset()));
                }
                if (j == 2) {
                    orcVo.setOffset(Double.valueOf(dataOffsetVo.getAvgOffset()));
                }
                orcVo.setAcTime(new Timestamp(sdfm.parse(dataOffsetVo.getMinZone()).getTime()));
                dataOffsetList.add(orcVo);
            }
        }
        return dataOffsetList;
    }

    /**
     * 解析倾角值
     */
    private CoordinateRadian parseCoordinateRadian(double x, double y) {
        return new CoordinateRadian(MyUtils.toRadian(x), MyUtils.toRadian(y));
    }

    /**
     * 清空文件，以便作删除特征
     *
     * @param file
     * @throws FileNotFoundException
     */
    public void clearFile(File file) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(file);
        pw.close();
    }

}
