package ru.viljinsky.reports;

import ru.viljinsky.sqlite.Column;
import ru.viljinsky.sqlite.Dataset;
import ru.viljinsky.sqlite.Values;


public class ReportBuilder{
    
    public static ReportInfo[] reportInfoList = {
        new ReportInfo(ReportInfo.RP_HOME),
        new ReportInfo(ReportInfo.RP_CURRICULUM),
        new ReportInfo(ReportInfo.RP_SCHEDULE_VAR_1),
        new ReportInfo(ReportInfo.RP_SCHEDULE_VAR_2),
        new ReportInfo(ReportInfo.RP_SCHEDULE_TEACHER),
        new ReportInfo(ReportInfo.RP_SCHEDULE_ERRORS),
        
    };
    
    
    public static String getReportName(String urlPath){
        if (urlPath.equals("/"))
            return ReportInfo.RP_HOME;
        if (urlPath.startsWith("/"))
            urlPath = urlPath.substring(1);
        for (ReportInfo info:reportInfoList){
            if (info.page.equals(urlPath))
                return info.getReportName();
        }
        return null;
    }
    
    
    /**
     * Поллучение HTML кода котента отчёта
     * @param info reportInfo
     * @return
     * @throws Exception 
     */
    public String getReport(ReportInfo info) throws Exception{
        if (info.getPage().equals("/"))
            return getIndexPage();
        AbstractReport report ;
        Class reportClass = info.getReportClass();
        if (reportClass!=null){
            report =  (AbstractReport)reportClass.newInstance();
            report.prepare();
            return report.getHtml();
        }
        return null;
    }
    
    public static final String HTML_PATTERN = 
        "<!DOCTYPE html>"+
        "<html lang='ru'>"+
        "<head>"+
        "$STYLE$"+
        "</head>"+
        "<body>"+
        
        "$NAVIGATOR$"+    
       
        "$CONTENT$"+   
        
        "<div font='small' align='center'><a href='http://www.составительрасписания.рф'>Составитель расписания</a> &copy; 2015</div>"+    
        "</body>"+
        "</html>";
    
   public static final String STYLE = 
         "<style> table{width:90%;border:solid 1px silver;border-collapse:collapse;}"+
         "td, th{border: solid 1px silver;}\n" +
         "th{color:#707070; background:#eeeeee;}"+
           
         "</style>";

    
   public static String createPage(final String reportContent){
       PageProducer producer = new PageProducer(){

           @Override
           public String getReplaceText(String tag) {
               switch (tag){
                   case"$STYLE$":
                       return STYLE;
                   case "$NAVIGATOR$":
                       return getNavigator();
                   case "$CONTENT$":
                       return reportContent;
                   default:
                       return "";
               }
           }
           
       };
       producer.setHtmlPattern(HTML_PATTERN);
       String result = producer.execute();
       return result;
   }
   
    
    public static String getNavigator(){
        StringBuilder result = new StringBuilder();
        result.append("<ul class='navigator'>");
        for (ReportInfo info:reportInfoList){
            result.append("<li><a href='"+info.page+"'>"+info.title+"</a></li>");
        }
        result.append("</ul>");
        return result.toString();
    }
    
    
    public String getIndexPage(){
        return "<h1>Пример расписания</h1>";
    }
    
}

/**
 * Абстрактный отчет расписания
 * 
 * @author вадик
 */
abstract class AbstractReport {
    
    protected static final String SQL_ERROR_HALL_IN_SCHEDULE =
            "select d.day_caption,l.time_start || ' ' || l.time_end as lesson_tyme,\n"+
            "                           b.label,a.day_id,a.bell_id,b.id as depart_id \n" +
            "from shift_detail a inner join depart b	on a.shift_id=b.shift_id\n" +
            "	inner join day_list d on a.day_id=d.day_no\n" +
            "	inner join bell_list l on l.bell_id=a.bell_id\n" +
            "where a.bell_id<(select max(bell_id) from schedule where day_id=a.day_id and depart_id=b.id)\n" +
            "   and not exists (select * \n" +
            "	from schedule where day_id=a.day_id and bell_id=a.bell_id and depart_id=b.id);";
    
    String html = null;
    
   /**
    * Заголовок отчётов 
    * Название учебного заведения
    * Учебный период
    * Период расписания
    * @return 
    */
   protected String getReportHeader(){
        String result = "Report header";

        try{
           result = ScheduleParams.getStringParamByName(ScheduleParams.SCHEDULE_SPAN)+"<br>"+
            ScheduleParams.getStringParamByName(ScheduleParams.SCHEDULE_TITLE)+"<br>"+
            ScheduleParams.getStringParamByName(ScheduleParams.EDUCATIONAL_INSTITUTION)+"<br>"+

            ScheduleParams.getStringParamByName(ScheduleParams.DATE_BEGIN)+"&nbsp;"+
            ScheduleParams.getStringParamByName(ScheduleParams.DATE_END)+"<br>";
        } catch (Exception e){
           e.printStackTrace();
        }

        return result;
    }

   /**
    * Преобразование dataseta в простую HTML таблицу
    * @param dataset
    * @return
    * @throws Exception 
    */
   protected String datasetToHtml(Dataset dataset) throws Exception{
        StringBuilder result = new StringBuilder();
        Values values;
        if (!dataset.isActive()){
            throw new Exception("DATASET_NOT_ACTIVE");
        }
        
        result.append("<table>");
        result.append("<tr>");
        
        for (Column column:dataset.getColumns()){
            result.append("<th>")
            .append(column.getColumnName())
            .append("</th>");
        }
        
        result.append("</tr>");
        
        for (int i=0;i<dataset.size();i++){
            result.append("<tr>");
            values=dataset.getValues(i);
            for (Column column:dataset.getColumns()){
                result.append("<td>")
                    .append(values.getString(column.getColumnName()))
                    .append("</td>");
            }
            
            result.append("</tr>");
        }
        result.append("</table>");
        return result.toString();
    }
   
    public abstract void prepare() throws Exception;

    public String getHtml(){
        return html;
    }
    
   
}
