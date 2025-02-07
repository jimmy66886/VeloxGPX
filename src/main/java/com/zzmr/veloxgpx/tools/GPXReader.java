package com.zzmr.veloxgpx.tools;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Length;
import io.jenetics.jpx.WayPoint;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class GPXReader {
    public static void main(String[] args) throws Exception {
        // 读取 GPX 文件
//        GPX gpx = GPX.read(Path.of("D:\\骑行记录\\春节500km.gpx"));
        GPX gpx = GPX.read(Path.of("D:\\骑行记录\\想去三州田水库150km.gpx"));

        // 获取所有轨迹点
        List<WayPoint> points = gpx.getTracks().get(0).getSegments().get(0).getPoints();

        if (points.isEmpty()) {
            System.out.println("GPX 文件没有轨迹点！");
            return;
        }

        // 初始化统计变量
        double totalDistance = 0.0;
        double totalAscent = 0.0;
        double totalDescent = 0.0;
        double movingTime = 0.0; // 移动时间（秒）
        Instant startTime = points.get(0).getTime().orElse(null).toInstant();
        Instant endTime = points.get(points.size() - 1).getTime().orElse(null).toInstant();

        WayPoint prevPoint = null;
        for (WayPoint point : points) {
            if (prevPoint != null) {
                // 计算两点间距离
                double distance = haversine(
                        prevPoint.getLatitude().doubleValue(), prevPoint.getLongitude().doubleValue(),
                        point.getLatitude().doubleValue(), point.getLongitude().doubleValue()
                );
                totalDistance += distance;

                // 计算海拔变化
                double elevationChange = point.getElevation()
                        .map(Length::doubleValue)
                        .orElse(0.0) - prevPoint.getElevation()
                        .map(Length::doubleValue)
                        .orElse(0.0);

                if (elevationChange > 0) {
                    totalAscent += elevationChange; // 爬升
                } else {
                    totalDescent += Math.abs(elevationChange); // 下降
                }

                // 计算移动时间
                Instant prevTime = prevPoint.getTime().orElse(null).toInstant();
                Instant currTime = point.getTime().orElse(null).toInstant();
                if (prevTime != null && currTime != null) {
                    double timeDiff = Duration.between(prevTime, currTime).toSeconds();
                    double speed = (distance / 1000) / (timeDiff / 3600); // km/h
                    if (speed > 1.0) { // 速度超过 1km/h 视为移动
                        movingTime += timeDiff;
                    }
                }
            }
            prevPoint = point;
        }

        // 输出统计结果
        System.out.println("总时间: " + (startTime != null && endTime != null ? Duration.between(startTime, endTime).toMinutes() + " 分钟" : "未知"));
        System.out.println("总时间: " +
                (startTime != null && endTime != null
                        ? String.format("%.2f", Duration.between(startTime, endTime).toMinutes() / 60.0) + " 小时"
                        : "未知"));
        System.out.println("移动时间: " + (int) (movingTime / 60) + " 分钟");
        System.out.printf("移动时间: %.2f 小时\n", movingTime / 3600);
        System.out.printf("累计爬升: %.2f 米\n", totalAscent);
        System.out.printf("累计下降: %.2f 米\n", totalDescent);
        System.out.printf("总路程: %.2f 公里\n", totalDistance / 1000);
    }

    // Haversine 公式计算地球表面两点间距离（单位：米）
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

