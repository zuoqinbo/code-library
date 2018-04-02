import java.util.Random;

/**
 * Description
 *
 * @author qinbo.zuo
 * @create 2018-04-02 14:12
 **/
public class VerificationCode {

    private String generateVerificationCode() {
        Random r = new Random(System.currentTimeMillis());
        int tag[] = {0, 0, 0, 0, 0, 0};
        String vcode = "";
        int temp;
        while (vcode.length() < 6) {
            temp = r.nextInt(6);
            if (tag[temp] == 0) {
                int radomNum = r.nextInt(10);
                vcode = vcode + radomNum;
                tag[temp] = 1;
            }
        }
        return vcode;
    }

}
