import java.util.Arrays;

public class test {
    public static void main(String[] args) {
        OnlineCoursesAnalyzer analyzer = new OnlineCoursesAnalyzer("local.csv");
        analyzer.recommendCourses(10,0,1);
    }
}
