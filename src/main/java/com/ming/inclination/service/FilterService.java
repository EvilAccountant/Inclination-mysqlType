package com.ming.inclination.service;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import com.ming.inclination.entity.TblFilterOffset;
import com.ming.inclination.entity.TblOriginOffset;
import com.ming.inclination.util.MyUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@SuppressWarnings("unchecked")
public class FilterService {

    /**
     * 将数据滤波并保存
     * X Y数据分开按ID分组，一共 canNumber*2 组数据进行滤波
     *
     * @param offsetList
     * @param filteredList
     */
    public void throughFilter(List<TblOriginOffset> offsetList, List<TblFilterOffset> filteredList, int canNumber, String[] canIdArr) {
        //按ID存放数据
        LinkedHashMap<String, List<Double>> canMapOVX = initCanMap(canNumber, canIdArr);
        LinkedHashMap<String, List<Double>> canMapOVY = initCanMap(canNumber, canIdArr);
        //计数MAP
        HashMap<String, Integer> dataXCount = new HashMap<>(canNumber);
        HashMap<String, Integer> dataYCount = new HashMap<>(canNumber);
        //存储滤波后数据
        List<double[]> tempListX = new ArrayList<>(canNumber);
        List<double[]> tempListY = new ArrayList<>(canNumber);
        double tempDataX, tempDataY;
        String tempId;
        int idTail;
        for (int i = 0; i < canNumber; i++) {
            dataXCount.put(canIdArr[i], 0);
            dataYCount.put(canIdArr[i], 0);
        }

        for (int i = 0; i < offsetList.size(); i++) {
            tempId = offsetList.get(i).getCanId();
            tempDataX = offsetList.get(i).getValueX();
            tempDataY = offsetList.get(i).getValueY();
            canMapOVX.get(tempId).add(tempDataX);
            canMapOVY.get(tempId).add(tempDataY);
        }

        for (List<Double> oriList : canMapOVX.values()) {
            tempListX.add(lowPassFilter(oriList));
        }

        for (List<Double> oriList : canMapOVY.values()) {
            tempListY.add(lowPassFilter(oriList));
        }

        for (TblOriginOffset originOffset : offsetList) {
            tempId = originOffset.getCanId();
            idTail = Integer.valueOf(tempId.substring(5)) - 1;

            TblFilterOffset filteredOffset = new TblFilterOffset();
            filteredOffset.setId(MyUtils.generateUUID());
            filteredOffset.setCanId(tempId);
            filteredOffset.setDataTime(originOffset.getDataTime());
            filteredOffset.setOriValueX(originOffset.getValueX());
            filteredOffset.setOriValueY(originOffset.getValueY());
            filteredOffset.setValueX(tempListX.get(idTail)[dataXCount.get(tempId)]);
            filteredOffset.setValueY(tempListY.get(idTail)[dataYCount.get(tempId)]);
            //计数map后移一位
            dataXCount.put(tempId, dataXCount.get(tempId) + 1);
            dataYCount.put(tempId, dataYCount.get(tempId) + 1);
            filteredList.add(filteredOffset);
        }
    }

    /**
     * 初始化各canId的数组Map
     *
     * @return
     */
    private LinkedHashMap<String, List<Double>> initCanMap(int canNumber, String[] canArr) {
        LinkedHashMap<String, List<Double>> canMap = new LinkedHashMap<>(canNumber);
        for (int i = 0; i < canNumber; i++) {
            canMap.put(canArr[i], new ArrayList<>());
        }
        return canMap;
    }

    /**
     * 滤波处理
     * @param signalList
     * @return
     */
    public double[] lowPassFilter(List<Double> signalList) {
        IirFilterCoefficients iirFilterCoefficients = IirFilterDesignExstrom.design(FilterPassType.lowpass, 10, 0.1, 0.1);
        double[] signal = signalList.stream().mapToDouble(d -> d).toArray();
        return IIRFilter(signal,iirFilterCoefficients.a,iirFilterCoefficients.b);
    }

    public synchronized double[] IIRFilter(double[] signal, double[] a, double[] b) {
        double[] in = new double[b.length];
        double[] out = new double[a.length - 1];
        double[] outData = new double[signal.length];

        for (int i = 0; i < signal.length; i++) {

            System.arraycopy(in, 0, in, 1, in.length - 1);
            in[0] = signal[i];
            //calculate y based on a and b coefficients
            //and in and out.
            float y = 0;
            for (int j = 0; j < b.length; j++) {
                y += b[j] * in[j];
            }

            for (int j = 0; j < a.length - 1; j++) {
                y -= a[j + 1] * out[j];
            }
            //shift the out array
            System.arraycopy(out, 0, out, 1, out.length - 1);
            out[0] = y;
            outData[i] = y;
        }
        return outData;
    }
}
