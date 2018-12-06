package org.eclipse.kura.stormseeker.led;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LedStatus implements ConfigurableComponent, CloudConnectionListener, CloudSubscriberListener, CloudDeliveryListener {
		
	//Variables
	private static final Logger s_logger = LoggerFactory.getLogger(LedStatus.class);
	
	//Property Name
	private static final String THRESHOLD = "threshold.value"; 
	private static final String NAME = "name";
	
	//private Map<String, Object> properties;
	private Map<String, Object> properties;
	private boolean ledStatus;
	
	private CloudSubscriber cloudSubscriber;
	private CloudPublisher cloudPublisher;
	private final ScheduledExecutorService worker;
	private ScheduledFuture<?> handle;

	//Constructor
	public LedStatus() {
		super();
		this.worker = Executors.newSingleThreadScheduledExecutor();
	}
	
	//Subscriber set and unset methods
	public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
		this.cloudSubscriber = cloudSubscriber;
		this.cloudSubscriber.registerCloudSubscriberListener(LedStatus.this);
		this.cloudSubscriber.registerCloudConnectionListener(LedStatus.this);
	}
	
	public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
		this.cloudSubscriber.unregisterCloudSubscriberListener(LedStatus.this);
		this.cloudSubscriber.unregisterCloudConnectionListener(LedStatus.this);
		this.cloudSubscriber = null;
	}
	
	//Publisher set and unset methods
	public void setCloudPublisher(CloudPublisher cloudPublisher) {
		this.cloudPublisher = cloudPublisher;
		this.cloudPublisher.registerCloudConnectionListener(LedStatus.this);
		this.cloudPublisher.registerCloudDeliveryListener(LedStatus.this);
	}
	
	public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
		this.cloudPublisher.unregisterCloudConnectionListener(LedStatus.this);
		this.cloudPublisher.unregisterCloudDeliveryListener(LedStatus.this);
		this.cloudPublisher = null;
	}
	
	//Activation API
	
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating Led...");
		//updated(properties);
		this.properties = properties;
		for (Entry<String, Object> property : properties.entrySet()) {
			s_logger.info("Update - {}: {}", property.getKey(), property.getValue());
		}

		// get the mqtt client for this application
		try {
			// Don't subscribe because these are handled by the default
			// subscriptions and we don't want to get messages twice
			doUpdate(false);
		} catch (Exception e) {
			s_logger.error("Error during component activation", e);
			throw new ComponentException(e);
		}
		s_logger.info("Activating Temp... Done.");

	}
	
	protected void deactivate(ComponentContext componentContext) {
		s_logger.debug("Deactivating Led");

		this.worker.shutdown();
	}
	
	public void updated(Map<String, Object> properties) {

		this.properties = properties;
		for (Entry<String, Object> property : properties.entrySet()) {
			s_logger.info("Update - {}: {}", property.getKey(), property.getValue());
		}
		// try to kick off a new job
		doUpdate(true);
		s_logger.info("Updated Temp... Done.");
	}
	
	//Cloud Application Callback Methods 
	
	@Override
	public void onConnectionEstablished() {
		s_logger.info("Connection established");
	}

	@Override
	public void onConnectionLost() {
		s_logger.warn("Connection lost!");
	}

	@Override
	public void onDisconnected() {
		s_logger.warn("On disconnected");
	}

	@Override
	public void onMessageConfirmed(String messageId) {
		s_logger.info("Confirmed message with id: {}", messageId);
	}
	
	//Method called everytime a new message arrives
	@Override
	public void onMessageArrived(KuraMessage message) {
		//Receive the message
		logReceivedMessage(message);
		//Publish the led.status
		//doPublish();
		//doUpdate(true);
	}
	
	//Private Methods Subscriber
	
	private void logReceivedMessage(KuraMessage msg) {
		KuraPayload payload = msg.getPayload();
		Date timestamp = payload.getTimestamp();
		if (timestamp != null) {
			s_logger.info("Message timestamp: {}", timestamp.getTime());
		}

		if (payload.metrics() != null) {
			for (Entry<String, Object> entry : payload.metrics().entrySet()) {
				s_logger.info("Message metric: {}, value: {}", entry.getKey(), entry.getValue());
				
				//Verify if the temp value is bigger than the threshold
				if(entry.getKey().equals("value")) {
					//float temp = Float.parseFloat(entry.getValue());//entry.getValue(); 
					//float temp = (entry.getValue()).floatValue();
					//float temp = (float) (entry.getValue()).toString();
					float value = Float.parseFloat((entry.getValue().toString()));
					float thr = (float) this.properties.get(THRESHOLD);
					s_logger.info("value: {}, threshold: {}", value,thr);
					if (value>thr) {
						s_logger.info("value>thr");
						this.ledStatus = true;
					}else {
						s_logger.info("value>thr else");
						this.ledStatus = false;	
						}
					//s_logger.info("led_status:{}", this.ledStatus);
				}
			
			}
		}
	}
	
	//Private Methods Publisher
	
	private void doUpdate(boolean onUpdate) {
		//cancel a current worker handle if one is active
		//if (this.handle != null) {
		//	this.handle.cancel(true);
		//}

		//reset the temperature to the initial value
		//if (!onUpdate) {
			//verify if there is a need for change
		//	this.ledStatus = false;
		//}

		//change
		//int pubrate =5;
		int pubrate = 10;
		String name_2 = (String) this.properties.get(NAME);
		this.handle = this.worker.scheduleAtFixedRate(new Runnable() {
					
			@Override
			public void run() {
				
				
				String thrName = getClass().getSimpleName()+ name_2;	
				//Thread.currentThread().setName(getClass().getSimpleName());
				Thread.currentThread().setName(thrName);
				doPublish();
			}
		}, 0, pubrate, TimeUnit.SECONDS);
	}
	
	private void doPublish() {

		//default value for temperature
		//insert method to check temperature
		//this.temperature = 10;

		//test
		String name = (String) this.properties.get(NAME);
		
		if (this.cloudPublisher ==null) {
			//if(nonNull(this.cloudPublisher)) {
			s_logger.info("No cloud publisher selected. Temp Cannot publish!");
			return;
		}

		//Payload
		KuraPayload payload = new KuraPayload();

		payload.setTimestamp(new Date());
		boolean status = this.ledStatus;
		s_logger.info("doPublish status: {}",status);
		payload.addMetric("status", status);
		payload.addMetric("name", name);
		//Create Kura Message
		KuraMessage message = new KuraMessage(payload);

		//Publish the message
		try {
			this.cloudPublisher.publish(message);
			//s_logger.info("Publish message: {}", payload);
			s_logger.info("Publish message: ");
		} catch (Exception e) {
			s_logger.error("Cannot publish message: {}", message, e);
		}

	}
	
	
	
}
