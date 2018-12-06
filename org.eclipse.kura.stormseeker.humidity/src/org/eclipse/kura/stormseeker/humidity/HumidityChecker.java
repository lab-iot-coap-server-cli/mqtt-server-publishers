package org.eclipse.kura.stormseeker.humidity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HumidityChecker implements ConfigurableComponent, CloudConnectionListener, CloudDeliveryListener {
	
	//Variables
		private static final Logger s_logger = LoggerFactory.getLogger(HumidityChecker.class);

		private Map<String, Object> properties;

		private float humidity = 0;

		private CloudPublisher cloudPublisher;

		private final ScheduledExecutorService worker;

		private ScheduledFuture<?> handle;
		
		private String line;
		private String[] data;

		//Constructor
		public HumidityChecker() {
			super();
			this.worker = Executors.newSingleThreadScheduledExecutor();
		}

		//Set and unset methods
		public void setCloudPublisher(CloudPublisher cloudPublisher) {
			this.cloudPublisher = cloudPublisher;
			this.cloudPublisher.registerCloudConnectionListener(HumidityChecker.this);
			this.cloudPublisher.registerCloudDeliveryListener(HumidityChecker.this);
		}

		public void unsetCloudPublisher(CloudPublisher cloudPublisher) {
			this.cloudPublisher.unregisterCloudConnectionListener(HumidityChecker.this);
			this.cloudPublisher.unregisterCloudDeliveryListener(HumidityChecker.this);
			this.cloudPublisher = null;
		}
		
		//Activation API
		protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
			s_logger.info("Activating HumidityChecker...");
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
			s_logger.debug("Deactivating HumidityChecker");

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

			//reset the Humidity to the initial value
			//if (!onUpdate) {
				//verify if there is a need for change
			//	this.humidity = 10000;
			//}

			//change
			int pubrate =5;
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

			//default value for Humidity
			//insert method to check Humidity
			//this.humidity = 10;
			this.humidity = getHumidity();
			
			if (this.cloudPublisher ==null) {
				//if(nonNull(this.cloudPublisher)) {
				s_logger.info("No cloud publisher selected. Temp Cannot publish!");
				return;
			}

			//Payload
			KuraPayload payload = new KuraPayload();

			payload.setTimestamp(new Date());
			payload.addMetric("type", "humidity");
			payload.addMetric("value", this.humidity);
			payload.addMetric("measurement", "%");

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
		public float getHumidity() throws Exception{
			float hum = 10000;
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec("python /home/pi/teste_python_sensor.py");
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			///System.out.println(line);
			if ((line = bri.readLine())!= null) {
				data = line.split(";");
				//System.out.println(data);
				hum = Float.parseFloat(data[1]);
				
			}
			return hum;
		}
}
