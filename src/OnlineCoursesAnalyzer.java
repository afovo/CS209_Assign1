import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {
    ArrayList<Course> courses = new ArrayList<>();
    int recommendNum = 10;

    public OnlineCoursesAnalyzer(String datasetPath) {
        String line;
        String[]parts;
        try (BufferedReader file = new BufferedReader(new InputStreamReader(
                new FileInputStream(datasetPath), StandardCharsets.UTF_8))) {
            file.readLine();
            while ((line = file.readLine()) != null) {
                parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length == 23 && checkNull(parts)) {
                    Course c = new Course();
                    c.institution = parts[0];
                    c.courseNumber = parts[1];
                    c.launchDate = new Date(parts[2]);
                    c.courseTitle = parts[3].startsWith("\"")
                            ? parts[3].substring(1, parts[3].length() - 1) : parts[3];
                    c.instructors = parts[4].startsWith("\"")
                            ? parts[4].substring(1, parts[4].length() - 1).split(", ") : parts[4].split(", ");
                    c.courseSubject = parts[5].startsWith("\"")
                            ? parts[5].substring(1, parts[5].length() - 1) : parts[5];
                    c.year = Integer.parseInt(parts[6]);
                    c.honorCodeCertificates = Integer.parseInt(parts[7]);
                    c.participantCnt = Integer.parseInt(parts[8]);
                    c.auditedCnt = Integer.parseInt(parts[9]);
                    c.certifiedCnt = Integer.parseInt(parts[10]);
                    c.audited = Double.parseDouble(parts[11]);
                    c.certified = Double.parseDouble(parts[12]);
                    c.certifiedHalf = Double.parseDouble(parts[13]);
                    c.playedVideo = Double.parseDouble(parts[14]);
                    c.postedForum = Double.parseDouble(parts[15]);
                    c.gradeHigherThanZero = Double.parseDouble(parts[16]);
                    c.totalCourseHours = Double.parseDouble(parts[17]);
                    c.midHoursCert = Double.parseDouble(parts[18]);
                    c.midAge = Double.parseDouble(parts[19]);
                    c.male = Double.parseDouble(parts[20]);
                    c.female = Double.parseDouble(parts[21]);
                    c.isBachelor = Double.parseDouble(parts[22]);
                    courses.add(c);
                }
            }
        } catch (IOException e) {
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> mid =  courses.stream()
                .collect(Collectors.groupingBy(c -> c.institution, Collectors.summingInt(c -> c.participantCnt)));
        return mid.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> o, LinkedHashMap::new));
    }

    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> mid =  courses.stream()
                .collect(Collectors.groupingBy(c -> c.institution.concat("-").concat(c.courseSubject),
                        Collectors.summingInt(c -> c.participantCnt)));
        return mid.entrySet().stream()
                .sorted(Comparator.comparingInt(c -> -c.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> o, LinkedHashMap::new));
    }

    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        // <Instructor, [[course1, course2,...],[coursek,coursek+1,...]]>
        Map<String, List<List<String>>> res = new HashMap<>();
        for (Course c : courses) {
            for (String instructor : c.instructors) {
                if (!res.containsKey(instructor)) {
                    ArrayList<List<String>> i = new ArrayList<>();
                    i.add(new ArrayList<>());
                    i.add(new ArrayList<>());
                    res.put(instructor, i);
                }
                if (c.instructors.length > 1 && !res.get(instructor).get(1).contains(c.courseTitle)) {
                    res.get(instructor).get(1).add(c.courseTitle);
                } else if (c.instructors.length == 1 && !res.get(instructor).get(0).contains(c.courseTitle)) {
                    res.get(instructor).get(0).add(c.courseTitle);
                }
            }
        }
        for (String instructor : res.keySet()) {
            Collections.sort(res.get(instructor).get(0));
            Collections.sort(res.get(instructor).get(1));
        }
        return res;
    }

    public List<String> getCourses(int topK, String by) {
        switch (by) {
            case "hours":
                return courses.stream().sorted((c1, c2) -> {
                    if (c1.totalCourseHours < c2.totalCourseHours) {
                        return 1;
                    } else if (c1.totalCourseHours > c2.totalCourseHours) {
                        return -1;
                    } else {
                        return Character.compare(c1.courseTitle.charAt(0),
                                c2.courseTitle.charAt(0));
                    }
                }).map(c -> c.courseTitle).distinct().limit(topK).collect(Collectors.toList());
            case "participants":
                return courses.stream().sorted((c1, c2) -> {
                    if (c1.participantCnt < c2.participantCnt) {
                        return 1;
                    } else if (c1.participantCnt > c2.participantCnt) {
                        return -1;
                    } else {
                        return Character.compare(c1.courseTitle.charAt(0),
                                c2.courseTitle.charAt(0));
                    }
                }).map(c -> c.courseTitle).distinct().limit(topK).collect(Collectors.toList());
            default:
                break;
        }
        return null;
    }

    public List<String> searchCourses(String courseSubject, double
            percentAudited, double totalCourseHours) {
        return courses.stream()
                .filter(c -> c.courseSubject.toLowerCase(Locale.ROOT).contains(courseSubject.toLowerCase(Locale.ROOT)))
                .filter(c -> c.audited >= percentAudited)
                .filter(c -> c.totalCourseHours <= totalCourseHours)
                .map(c -> c.courseTitle)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        Map<String, List<Course>> coursesById = courses.stream()
                .collect(Collectors.groupingBy(c -> c.courseNumber, Collectors.toList()));
        Map<String, Double> courseRank = new HashMap<>();
        Double[] average = new Double[3];
        for (List<Course> courseList : coursesById.values()) {
            courseList.sort((c1, c2) -> Integer.compare(0, c1.launchDate.compareTo(c2.launchDate)));
            average[0] = courseList.stream().collect(Collectors.averagingDouble(c -> c.midAge));
            average[1] = courseList.stream().collect(Collectors.averagingDouble(c -> c.male));
            average[2] = courseList.stream().collect(Collectors.averagingDouble(c -> c.isBachelor));
            courseRank.put(courseList.get(0).courseTitle,
                    getSimilarity(age, gender, isBachelorOrHigher, average));
        }
        ArrayList<Map.Entry<String, Double>> mappingList = new ArrayList<>(courseRank.entrySet());
        mappingList.sort((e1, e2) -> {
            if (e1.getValue() < e2.getValue()){
                return -1;
            } else if (e1.getValue() > e2.getValue()) {
                return 1;
            } else  {
                return Integer.compare(0, e2.getKey().compareTo(e1.getKey()));
            }
        });
        ArrayList<String> res = new ArrayList<>();
        for (int i = 0; i < recommendNum; i++) {
            res.add(mappingList.get(i).getKey());
        }
        return res;
    }

    public boolean checkNull(String[]parts) {
        for (String s : parts) {
            if (s.length() == 0) {
                return false;
            }
        }
        return true;
    }

    public double getSimilarity(int age, int gender, int isBachelorOrHigher, Double[] averages) {
        return Math.pow(age - averages[0], 2)
                + Math.pow(gender * 100 - averages[1], 2)
                + Math.pow(isBachelorOrHigher * 100 - averages[2], 2);
    }
}

class Course {
    String institution; // - online course holders
    String courseNumber; //the unique id of each course
    Date launchDate; //- the launch date of each course
    String courseTitle; // - the title of each course
    String[]instructors; // - the instructors of each course
    String courseSubject; // - the subject of each course
    int year; // - the last time of each course
    int honorCodeCertificates; // - with (1), without (0).
    int participantCnt; // the number of participants who have accessed the course
    int auditedCnt; // the number of participants who have audited more than 50% of the course
    int certifiedCnt; // - Total number of votes
    double audited; // - the percent of the audited
    double certified; //
    double certifiedHalf; // percent of the certified with accessing the course more than 50% %
    double playedVideo; //- the percent of playing video
    double postedForum; //- the percent of posting in forum
    double gradeHigherThanZero; // - the percent of grade higher than zero
    double totalCourseHours; // (Thousands) - total course hours(per 1000)
    double midHoursCert; // - median hours for certification
    double midAge; // Median Age - median age of the participants
    double male; // % Male - the percent of the male
    double female; // % Female - the percent of the female
    double isBachelor; // % Bachelor's Degree or Higher - the percent of bachelor's degree of higher
}