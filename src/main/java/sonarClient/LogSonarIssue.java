package sonarClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

public class LogSonarIssue {

	public static void main(String args[]) {
		try {
			String url = "http://fxzs-jenkins:8100";
			String login = "admin";
			String password = "admin";

			SonarClient client = SonarClient.builder().url(url).login(login).password(password).build();

			IssueQuery query = IssueQuery.create();
			query.severities("MAJOR", "MINOR", "CRITICAL");
			IssueClient issueClient = client.issueClient();
			Issues issues = issueClient.find(query);
			List<Issue> issueList = issues.list();

			for (Issue element : issueList) {
				System.out.println(element.projectKey() + " " + element.componentKey() + " " + element.line() + " " + element.ruleKey() + " "
						+ element.severity() + " " + element.message());
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
}

class GetSonarDataBK {
	private static String filename = "C:/SonarIssues.xls";
	private static String url = "http://fxzs-jenkins:8100";
	private static String login = "admin";
	private static String password = "admin";
	private static String[] severity = {"BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO"};

	public static void main(String args[]) throws IOException {

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
			createExcel(workbook, issueList);

			try (FileOutputStream fileOut = new FileOutputStream(filename)) {
				workbook.write(fileOut);
			}
		}
		System.out.println("Your excel file has been generated!");
	}

	private static void addFilters(IssueQuery query, int pageIndex, int pageSize, String language, String severity) {
		query.pageIndex(pageIndex);
		query.pageSize(pageSize);
		query.languages(language);
		query.severities(severity);
	}

	private static void createExcel(HSSFWorkbook workbook, List<Issue> issueList) {

		HSSFSheet sheet = workbook.createSheet("FirstSheet");
		HSSFRow rowhead = sheet.createRow((short) 0);
		rowhead.createCell(0).setCellValue("Project Key");
		rowhead.createCell(1).setCellValue("Component");
		rowhead.createCell(2).setCellValue("Line");
		rowhead.createCell(3).setCellValue("Rule Key");
		rowhead.createCell(4).setCellValue("Severity");
		rowhead.createCell(5).setCellValue("Message");

		for (int i = 0; i < issueList.size(); i++) {
			HSSFRow row = sheet.createRow((short) i + 1);
			row.createCell(0).setCellValue(issueList.get(i).projectKey());
			row.createCell(1).setCellValue(issueList.get(i).componentKey());
			row.createCell(2).setCellValue(String.valueOf(issueList.get(i).line()));
			row.createCell(3).setCellValue(issueList.get(i).ruleKey());
			row.createCell(4).setCellValue(issueList.get(i).severity());
			row.createCell(5).setCellValue(issueList.get(i).message());
		}
	}

}
