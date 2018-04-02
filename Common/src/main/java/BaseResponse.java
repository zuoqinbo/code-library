
public class BaseResponse<T> {
    private String code;
    private String message;
    private T data;


    public BaseResponse(){
        code =  ResponseCodeEnum.OK.getCode();
        message = ResponseCodeEnum.OK.getMessage();
    }
    public static BaseResponse createOk() {
        return create(ResponseCodeEnum.OK);
    }

    public static BaseResponse create(ResponseCodeEnum codeEnum) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(codeEnum.getCode());
        baseResponse.setMessage(codeEnum.getMessage());
        return baseResponse;
    }

    public static BaseResponse create(ResponseCodeEnum codeEnum, String message) {
        BaseResponse baseResponse = new BaseResponse();
        baseResponse.setCode(codeEnum.getCode());
        baseResponse.setMessage(message);
        return baseResponse;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
