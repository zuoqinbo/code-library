public enum ResponseCodeEnum {
    OK("20000","请求成功"),
    FAILURE("40000","请求失败");
    private String code;
    private String message;

    ResponseCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
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

    public static String toEnumText() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ResponseCodeEnum codeEnum :ResponseCodeEnum.values()) {
            stringBuilder.append(codeEnum.code).append(":").append(codeEnum.message).append("\n");
        }
        return stringBuilder.toString();
    }
}
