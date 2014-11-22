

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.auth.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;

/**
 * Servlet implementation class index
 */
@WebServlet(urlPatterns={"/index"}, asyncSupported=true)
public class index extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public index() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    private final Queue<AsyncContext> ongoingRequests = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService service;
    
    private AmazonEC2         ec2;
    private AmazonDynamoDB dynamo;
    private String pDate;
    private String pKeystring;
    
    
	
	
    @Override
    public void init(ServletConfig config) throws ServletException {
      final Runnable notifier = new Runnable() {
        @Override
        public void run() {
          final Iterator<AsyncContext> iterator = ongoingRequests.iterator();
          //not using for : in to allow removing items while iterating
          while (iterator.hasNext()) {
            AsyncContext ac = iterator.next();
            //Random random = new Random();
            final ServletResponse res = ac.getResponse();
            PrintWriter out;
            try {
              out = res.getWriter();
              if (dynamo == null) {
                  AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
                  dynamo = new AmazonDynamoDBClient(credentialsProvider);
              }
              ArrayList<String> arr = new ArrayList<String>();
              DateFormat cf = new SimpleDateFormat("MM/dd/yyyy");
          	  String currDate = cf.format(new Date());
          	  if(pDate != null && !pDate.isEmpty())
          		  currDate = pDate;
          	  /*String pKey;
          	  if(pKeystring != null && !pKeystring.isEmpty())	  
          		  	pKey = pKeystring;*/
          	  
          	  Condition keyCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString()).withAttributeValueList(new AttributeValue().withS(pKeystring));
              Condition dateCondition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString()).withAttributeValueList(new AttributeValue().withS(currDate));
              Map<String,Condition> scanCondition = new HashMap<String,Condition>();
              scanCondition.put("Date", dateCondition);
              if(pKeystring != null && !pKeystring.isEmpty())
            	  scanCondition.put("KeyString", keyCondition);
              ScanRequest scanRequest = new ScanRequest().withTableName("TweetInfo").withAttributesToGet(new String []{"KeyString","Date","Longtitude","Latitude"}).withScanFilter(scanCondition);
              
              ScanResult result = dynamo.scan(scanRequest);
          	  for(Map<String, AttributeValue> item : result.getItems())
          	  {
          		  if(item.values().size() == 4)
          		  arr.add(item.values().toString().replace("'", "").replace("{S: ", "'").replace(",}", "'"));
          	  };
              DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
              Date now = new Date();
      		  String reportDate = df.format(now);
              String next = "data: " + arr + "\n\n";
                //"num of clients = " + ongoingRequests.size() + "\n\n";// +
            		  //"array: " + "arr.toString()" + "\n\n";
              out.write(next);
              //out.write("array: " + "arr.toString()" + "\n\n");
              //checkError calls flush, 
              //and flush() does not throw IOException
              if (out.checkError()) { 
                iterator.remove();
              }
            } catch (IOException ignored) {
              iterator.remove();
            }
          }
        }
      };
      service = Executors.newScheduledThreadPool(1);
      service.scheduleAtFixedRate(notifier, 1, 20, TimeUnit.SECONDS);
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/event-stream");
		response.setCharacterEncoding("UTF-8");
		request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
		//pDate = request.getParameter("date");
		final AsyncContext ac = request.startAsync();
		ac.setTimeout(0);
		ac.addListener(new AsyncListener() {
		      @Override public void onComplete(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onTimeout(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onError(AsyncEvent event) throws 
		        IOException {ongoingRequests.remove(ac);/**/}
		      @Override public void onStartAsync(AsyncEvent event) throws 
		         IOException {}
		    });
		//while(true){
		ongoingRequests.add(ac);
		//}
		/*PrintWriter writer = response.getWriter();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		//for(int i=0; i<10; i++) {
		
 
            try {
		while(true){
		Date now = new Date();
		String reportDate = df.format(now);
            writer.write("data: "+ reportDate + "\n\n");
        //writer.close();
                Thread.sleep(1000);
		}
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        //}
            //}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//String date = request.getParameter("date");
		pKeystring = request.getParameter("key");
		//request.setAttribute("date", date);
		pDate = request.getParameter("date");
		request.getRequestDispatcher("index.jsp").forward(request,response);
		//doGet(request,response);
	}

}
