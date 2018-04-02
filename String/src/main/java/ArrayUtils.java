import java.util.Arrays;

/**
 * Description
 *
 * @author qinbo.zuo
 * @create 2018-04-02 15:00
 **/
public class ArrayUtils {

    public static void BatchCapacity() {
        int count = 150;//149 151分别进行测试
        String[] array = new String[count];
        int capacity = 50;
        int batch;
        if (count % capacity == 0) {
            batch = count / capacity;
        } else {
            batch = count / capacity + 1;
        }
        for (int i = 0; i < batch; i++) { // 分批次执行
            if ((count - i * capacity) <= capacity) {// 如果是余出的一部分 就是最后一批次
                String[] temp = Arrays.copyOfRange(array, (batch - 1) * capacity, array.length);
                System.out.println("最后批次 第" + (i + 1) + "批次");
            } else {// 整除批次 即每一批次
                String[] temp = Arrays.copyOfRange(array, i * capacity, capacity * (i + 1));
                System.out.println("第" + (i + 1) + "批次");
            }
        }
    }
}
