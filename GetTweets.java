import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

public class GetTweets {
	
	
	public static void main(String[] args) {
		AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (My Folder), and is in valid format.",
                    e);
        }
		AmazonDynamoDBClient client = new AmazonDynamoDBClient(new ProfileCredentialsProvider());
		

		
		/*HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put("TweetId", new AttributeValue().withS("0"));
		key.put("KeyString", new AttributeValue().withS("Hahaha"));
		GetItemRequest getItemRequest = new GetItemRequest()
	    .withTableName(tableName)
	    .withKey(key);

	GetItemResult result = client.getItem(getItemRequest);
	Map<String, AttributeValue> map = result.getItem();
	System.out.println();*/
		ConfigurationBuilder cb = new ConfigurationBuilder();
	    cb.setDebugEnabled(true);
	    cb.setOAuthConsumerKey("OsKJ9ILJls70tmIEU2g2yULgu");
	    cb.setOAuthConsumerSecret("P9aArdYM9JMKE6eKk5cxTy5Nv9brWrSOEWBp8mf0I2Uk0m1KnZ");
	    cb.setOAuthAccessToken("2847059842-Am6r9tjdlZFJJZEPFbv3fE7aAnhoSoDEBq5JpbO");
	    cb.setOAuthAccessTokenSecret("556Lfrk9JQEx4o6gwDJWDZTfFcn1s5sjaZStw2AGzYRct");
	
	    TwitterFactory tf = new TwitterFactory(cb.build());
	    twitter4j.Twitter twitter = tf.getInstance();
		//twitter4j.Twitter twitter = TwitterFactory.getSingleton();
	    String[] QueryList = {"NFL", "BaseBall", "Coffee", "New Arrivals", "Believe"};
	    for(String q : QueryList)
	    {
		    Query query = new Query(q);
		    QueryResult result;
			try {
				result = twitter.search(query);
			    for (Status status : result.getTweets()) {
			    	PutRecordDynamo(client, status, q);
			    }
			} catch (TwitterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	private static void PutRecordDynamo(AmazonDynamoDBClient pClient, Status pStatus, String pKeyString) 
	{
		String tableName = "TweetInfo";
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		if(pStatus.getGeoLocation() != null)
    	{
			if(!String.valueOf(pStatus.getGeoLocation().getLongitude()).isEmpty() && !String.valueOf(pStatus.getGeoLocation().getLatitude()).isEmpty())
			{
				item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
				item.put("KeyString", new AttributeValue().withS(pKeyString));
				item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
				item.put("Message", new AttributeValue().withS(pStatus.getText()));
				item.put("Longtitude", new AttributeValue().withS(String.valueOf(pStatus.getGeoLocation().getLongitude()).isEmpty()?"EMPTY": String.valueOf(pStatus.getGeoLocation().getLongitude())));
				item.put("Latitude", new AttributeValue().withS(String.valueOf(pStatus.getGeoLocation().getLatitude()).isEmpty()?"EMPTY": String.valueOf(pStatus.getGeoLocation().getLatitude())));
				//item.put("Location", new AttributeValue().withS(pStatus.getUser().getLocation().isEmpty()?"EMPTY":pStatus.getUser().getLocation()));
			}
    	}
		else if(!pStatus.getUser().getLocation().isEmpty())
		{
			String[] locations = pStatus.getUser().getLocation().split(",");
			if(locations.length == 2)
			{
				try{
					double latitude = Double.parseDouble(locations[0]);
					double longtitude = Double.parseDouble(locations[1]);
					item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
					item.put("KeyString", new AttributeValue().withS(pKeyString));
					item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
					item.put("Message", new AttributeValue().withS(pStatus.getText()));
					item.put("Longtitude", new AttributeValue().withS(String.valueOf(latitude)));
					item.put("Latitude", new AttributeValue().withS(String.valueOf(longtitude)));
				}catch(Exception e) 
				{
					
				}
			}
			//item.put("TweetId", new AttributeValue().withS(String.valueOf(pStatus.getId())));
			//item.put("KeyString", new AttributeValue().withS(pKeyString));
			//item.put("Date", new AttributeValue().withS(df.format(pStatus.getCreatedAt())));
			//item.put("Message", new AttributeValue().withS(pStatus.getText()));
			//item.put("Longtitude", new AttributeValue().withS("EMPTY"));
			//item.put("Latitude", new AttributeValue().withS("EMPTY"));
			//item.put("Location", new AttributeValue().withS(pStatus.getUser().getLocation()));
		}
		
		if(item.size() > 0)
		{
			PutItemRequest putItemRequest = new PutItemRequest()
			  .withTableName(tableName)
			  .withItem(item);
			PutItemResult Putresult = pClient.putItem(putItemRequest);
			System.out.println("@" + pStatus.getUser().getScreenName() + ":" + pStatus.getText());
		}
	}
}
