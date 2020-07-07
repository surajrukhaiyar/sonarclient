package sonarClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DateUtil;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.jsonsimple.JSONArray;
import org.sonar.wsclient.jsonsimple.JSONObject;
import org.sonar.wsclient.jsonsimple.parser.JSONParser;
import org.sonar.wsclient.jsonsimple.parser.ParseException;

//http://fxzs-jenkins:8100/api/issues/search?componentKeys=Zerobase-WIP&ps=500&p=1
//http://fxzs-jenkins:8100/api/rules/search?componentKeys=Zerobase-WIP&ps=500&p=1

public class GetSonarData {
	private static String filename = "C:/SonarIssues.xls";
	private static String url = "http://fxzs-jenkins:8100";
	private static String login = "admin";
	private static String password = "admin";
	private static String[] severity = {"BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"};

	public static void main(String[] args) throws IOException, ParseException {
		GetSonarData sonarData = new GetSonarData();
		Map<String, String> map = sonarData.getAllRules();
		sonarData.getJavaViolations(map);
	}

	private void getJavaViolations(Map<String, String> ruleMap) throws IOException {
		try (HSSFWorkbook workbook = new HSSFWorkbook()) {
			SonarClient client = SonarClient.builder().url(url).login(login).password(password).build();
			List<Issue> issueList = new ArrayList<>();
			for (String sev : severity) {
				for (int page = 1; page < 20; page++) {
					IssueQuery query = IssueQuery.create();
					addFilters(query, page, 500, "java", sev);
					IssueClient issueClient = client.issueClient();
					Issues issues = issueClient.find(query);
					List<Issue> issue = issues.list();
					if (issue.isEmpty()) {
						break;
					}
					issueList.addAll(issues.list());
				}
			}
			createExcel(workbook, issueList, ruleMap);
			writeViolationsCount(workbook, issueList, ruleMap);
			writeTypeCount(workbook, issueList, ruleMap);

			try (FileOutputStream fileOut = new FileOutputStream(filename)) {
				workbook.write(fileOut);
			}
		}
		System.out.println("Your excel file has been generated!");
	}

	private void addFilters(IssueQuery query, int pageIndex, int pageSize, String language, String severity) {
		query.pageIndex(pageIndex);
		query.pageSize(pageSize);
		query.languages(language);
		query.severities(severity);
		query.resolved(false);
		query.createdBefore(DateUtil.parseYYYYMMDDDate("2020/06/19"));
	}

	private void createExcel(HSSFWorkbook workbook, List<Issue> issueList, Map<String, String> ruleMap) {

		HSSFSheet sheet = workbook.createSheet("FirstSheet");
		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Project Key");
		rowhead.createCell(1).setCellValue("Component");
		rowhead.createCell(2).setCellValue("Line");
		rowhead.createCell(3).setCellValue("Rule Key");
		rowhead.createCell(4).setCellValue("Rule Name");
		rowhead.createCell(5).setCellValue("Severity");
		rowhead.createCell(6).setCellValue("Message");
		rowhead.createCell(7).setCellValue("Type");

		int issueListSize = issueList.size();
		System.out.println("Issue List Size : " + issueListSize);
		for (int i = 0; i < issueListSize; i++) {
			HSSFRow row = sheet.createRow((short) i + 1);
			Issue issue = issueList.get(i);
			row.createCell(0).setCellValue(issue.projectKey());
			row.createCell(1).setCellValue(issue.componentKey());
			row.createCell(2).setCellValue(String.valueOf(issue.line()));
			row.createCell(3).setCellValue(issue.ruleKey());
			row.createCell(4).setCellValue(ruleMap.get(issue.ruleKey()).split(" : ")[0]);
			row.createCell(5).setCellValue(issue.severity());
			row.createCell(6).setCellValue(issue.message());
			row.createCell(7).setCellValue(ruleMap.get(issue.ruleKey()).split(" : ")[1]);
		}
	}

	private static void writeViolationsCount(HSSFWorkbook workbook, List<Issue> issueList, Map<String, String> ruleMap) {
		Map<String, Integer> issueCount = new HashMap<>();
		for (Issue issue : issueList) {
			if (issueCount.get(issue.ruleKey()) == null) {
				issueCount.put(issue.ruleKey(), 1);
			} else {
				issueCount.put(issue.ruleKey(), issueCount.get(issue.ruleKey()) + 1);
			}
		}

		HSSFSheet sheet = workbook.createSheet("Summary");
		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Rule ID");
		rowhead.createCell(1).setCellValue("Rule Name");
		rowhead.createCell(2).setCellValue("Rule Type");
		rowhead.createCell(3).setCellValue("Count");

		int currRow = 1;
		for (Entry<String, Integer> entry : issueCount.entrySet()) {
			HSSFRow row = sheet.createRow((short) currRow++);
			row.createCell(0).setCellValue(entry.getKey());
			row.createCell(1).setCellValue(ruleMap.get(entry.getKey()).split(" : ")[0]);
			row.createCell(2).setCellValue(ruleMap.get(entry.getKey()).split(" : ")[1]);
			row.createCell(3).setCellValue(entry.getValue());
		}
	}

	private static void writeTypeCount(HSSFWorkbook workbook, List<Issue> issueList, Map<String, String> ruleMap) {
		Map<String, Integer> typeCount = new HashMap<>();

		for (Issue issue : issueList) {
			String ruleType = ruleMap.get(issue.ruleKey()).split(" : ")[1];

			if (typeCount.get(ruleType) == null) {
				typeCount.put(ruleType, 1);
			} else {
				typeCount.put(ruleType, typeCount.get(ruleType) + 1);
			}
		}

		HSSFSheet sheet = workbook.createSheet("Type");
		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Type Name");
		rowhead.createCell(1).setCellValue("Count");

		int currRow = 1;
		for (Entry<String, Integer> entry : typeCount.entrySet()) {
			HSSFRow row = sheet.createRow((short) currRow++);
			row.createCell(0).setCellValue(entry.getKey());
			row.createCell(1).setCellValue(entry.getValue());
		}
	}

	// returns map of rulekey and ruleName
	public Map<String, String> getAllRules() throws ParseException {
		Map<String, String> paramMap = new HashMap<>();

		SonarClient client = SonarClient.builder().url(url).login(login).password(password).build();
		JSONArray ja = null;
		for (int page = 1; page < 20; page++) {
			String rules = client.get("/api/rules/search", "componentKeys", "Zerobase-WIP", "ps", 500, "p", page, "lang", "java");
			JSONObject obj = (JSONObject) new JSONParser().parse(rules);
			ja = (JSONArray) obj.get("rules");
			if (ja.isEmpty()) {
				break;
			}

			Iterator itr2 = ja.iterator();
			while (itr2.hasNext()) {
				Map currRule = ((Map) itr2.next());
				paramMap.put(currRule.get("key").toString(), currRule.get("name").toString() + " : " + currRule.get("type").toString());
			}
		}

		System.out.println("rules count :" + paramMap.size());
		return paramMap;
	}

}
