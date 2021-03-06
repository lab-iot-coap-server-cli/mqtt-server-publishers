package org.eclipse.kura.stormseeker.temperature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
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
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemperatureChecker implements ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {

	//Variables
	private static final Logger s_logger = LoggerFactory.getLogger(TemperatureChecker.class);

	private Map<String, Object> properties;

	private float temperature = 0;

	private CloudPublisher cloudPublisher;

	private final ScheduledExecutorService worker;

	private ScheduledFuture<?> handle;
	
	private String line;
	private String[] data;

	//Constructor
	public TemperatureChecker() {
		super();
		this.worker = Executors.newSingleThreadScheduledExecutor();
	}

	//Set and unset methods
	public void setCloudPublisher(CloudPublisher cloudPublisher) {
		this.cloudPublisher = cloudPublisher;
		this.cloudPublisher.registerCloudConnectionListener(TemperatureChecker.this);
		this.cloudPublisher.registerCloudDeliveryListener(TemperatureChecker.this);
	}

	public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
		this.cloudPublisher.unregisterCloudConnectionListener(TemperatureChecker.this);
		this.cloudPublisher.unregisterCloudDeliveryListener(TemperatureChecker.this);
		this.cloudPublisher = null;
	}
	
	//Activation API
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
		s_logger.info("Activating TemperatureChecker...");
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
		s_logger.debug("Deactivating TemperatureChecker");

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
	
	/// Cloud Application Callback Methods
	
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


	/// Private methods
	
	private void doUpdate(boolean onUpdate) {
		//cancel a current worker handle if one is active
		if (this.handle != null) {
			this.handle.cancel(true);
		}

		//reset the temperature to the initial value
		//if (!onUpdate) {
			//verify if there is a need for change
			//this.temperature = 0;
		//}

		//change
		//int pubrate =5;
		int pubrate = 10;
		this.handle = this.worker.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName(getClass().getSimpleName());
				try {
					doPublish();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0, pubrate, TimeUnit.SECONDS);
	}


	private void doPublish() throws Exception {

		//default value for temperature
		//insert method to check temperature
		//this.temperature = 10;
		this.temperature = getTemperature();
		
		if (this.cloudPublisher ==null) {
			//if(nonNull(this.cloudPublisher)) {
			s_logger.info("No cloud publisher selected. Temp Cannot publish!");
			return;
		}

		//Payload
		KuraPayload payload = new KuraPayload();

		payload.setTimestamp(new Date());
		payload.addMetric("type", "temperature");
		payload.addMetric("value", this.temperature);
		payload.addMetric("measurement", "Celsius");

		//Create Kura Message
		KuraMessage message = new KuraMessage(payload);

		//Publish the message
		try {
			this.cloudPublisher.publish(message);
			s_logger.info("Publish message: {}", payload);
		} catch (Exception e) {
			s_logger.error("Cannot publish message: {}", message, e);
		}

	}
	
	public float getTemperature() throws Exception{
		float temp= 10000;
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec("python /home/pi/teste_python_sensor.py");
		BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
		///System.out.println(line);
		if ((line = bri.readLine())!= null) {
			data = line.split(";");
			//System.out.println(data);
			//this.temperature = Float.parseFloat(data[0]);
			temp = Float.parseFloat(data[0]);
			
		}
		return temp;
		
	}
}
