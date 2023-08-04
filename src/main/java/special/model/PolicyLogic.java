package special.model;

/**
 * @author Luca Conte
 */


public record PolicyLogic<T>(String id, T expression){
    @Override
    public String toString() {
        return "PolicyLogic{\n" +
                "id='" + id + '\'' +
                ",\n expression=" + expression +
                "\n}";
    }
}
