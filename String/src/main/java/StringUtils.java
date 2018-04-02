import com.google.common.base.Joiner;

import java.io.*;
import java.util.List;

/**
 * Description
 *
 * @author qinbo.zuo
 * @create 2018-04-02 14:13
 **/
public class StringUtils {


    /**
     *
     * @param orderIdList
     * @return
     */
    public String convertListToString(List<String> orderIdList){
        return  Joiner.on(",").join(orderIdList);
    }

    public void readFileFromClassPath() throws IOException, ClassNotFoundException {
        InputStream in =  this.getClass().getClassLoader().getResourceAsStream("gs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        StringBuffer message = new StringBuffer();
        while((line= reader.readLine())!=null)
        {
            System.out.println(line);
            message.append(line);
        }

    }
}
