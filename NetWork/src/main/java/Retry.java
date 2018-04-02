/**
 * Description
 *
 * @author qinbo.zuo
 * @create 2018-04-02 14:30
 **/
public class Retry {

    private static final Integer COUNT = 3;

    public void retry() {
        Integer tryTimes = 0;
        boolean flag = true;
        while ((tryTimes < COUNT) && flag) {//重连机制 3次
            try {
                //TODO
                flag = false;
            } catch (Exception e) {
                tryTimes = tryTimes + 1;
            }
        }
    }
}
