import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        OnlineCoursesAnalyzer analyzer = new OnlineCoursesAnalyzer("local.csv");
        analyzer.recommendCourses(10, 0, 1);
    }
}
