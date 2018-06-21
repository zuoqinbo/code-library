import javafx.util.Pair;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author qinbo.zuo
 * @create 2018-06-12 11:28
 **/
public class WeekUtils {



    public static Pair<String, String> getWeekStartAndEndTime() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int d = 0;
        if (cal.get(Calendar.DAY_OF_WEEK) == 1) {
            d = -6;
        } else {
            d = 2 - cal.get(Calendar.DAY_OF_WEEK);
        }
        cal.add(Calendar.DAY_OF_WEEK, d);

        // 所在周开始日期
        String startTime = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        startTime = startTime + " 00:00:00";
        cal.add(Calendar.DAY_OF_WEEK, 6);
        // 所在周结束日期
        String endTime = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
        endTime = endTime + " 11:59:59";
        return new ImmutablePair<>(startTime, endTime);
    }
}