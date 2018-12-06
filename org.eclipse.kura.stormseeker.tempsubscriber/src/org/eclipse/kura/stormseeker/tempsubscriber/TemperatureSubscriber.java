package org.eclipse.kura.stormseeker.tempsubscriber;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
//import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class TemperatureSubscriber implements ConfigurableComponent, CloudConnectionListener, CloudSubscriberListener {

	//Variables
	private static final Logger s_logger = LoggerFactory.getLogger(TemperatureSubscriber.class);
	private Map<String, Object> properties;

	private CloudSubscriber cloudSubscriber;
	private CloudSubscriber cloudSubscriber2;
	private final ScheduledExecutorService worker;
	private ScheduledFuture<?> handle;
	
	private boolean tempLedStatus;
	private boolean humLedStatus;

	//GPIO Variables
	private GPIOService gpioService;
	//private GPIOService gpioServiceGreen;
	KuraGPIOPin ledRed;
	KuraGPIOPin ledGreen;
	//KuraGPIOPin led;
	//this.gpioService.getPinByTerminal(26);

	//Constructor
	public TemperatureSubscriber() throws KuraGPIODeviceException, KuraUnavailableDeviceException, IOException {
		super();
		this.worker = Executors.newSingleThreadScheduledExecutor();
		
		//led.open();
				
	}
	
	//GPIO methods
	public void setGPIOService(GPIOService gpioService) {
		this.gpioService = gpioService;
	}
	
	public void unsetGPIOService(GPIOService gpioService) {
		gpioService = null;
	}

	//Set and Unset first subscriber methods
	public void setCloudSubscriber(CloudSubscriber cloudSubscriber) {
		this.cloudSubscriber = cloudSubscriber;
		this.cloudSubscriber.registerCloudSubscriberListener(TemperatureSubscriber.this);
		this.cloudSubscriber.registerCloudConnectionListener(TemperatureSubscriber.this);
	}

	public void unsetCloudSubscriber(CloudSubscriber cloudSubscriber) {
		this.cloudSubscriber.unregisterCloudSubscriberListener(TemperatureSubscriber.this);
		this.cloudSubscriber.unregisterCloudConnectionListener(TemperatureSubscriber.this);
		this.cloudSubscriber = null;
	}
	
	//Set and Unset second subscriber methods
	public void setCloudSubscriber2(CloudSubscriber cloudSubscriber2) {
		this.cloudSubscriber2 = cloudSubscriber2;
		this.cloudSubscriber2.registerCloudSubscriberListener(TemperatureSubscriber.this);
		this.cloudSubscriber2.registerCloudConnectionListener(TemperatureSubscriber.this);
	}

	public void unsetCloudSubscriber2(CloudSubscriber cloudSubscriber2) {
		this.cloudSubscriber2.unregisterCloudSubscriberListener(TemperatureSubscriber.this);
		this.cloudSubscriber2.unregisterCloudConnectionListener(TemperatureSubscriber.this);
		this.cloudSubscriber2 = null;
	}

	//Activation API
	protected void activate(ComponentContext componentContext, Map<String, Object> properties) throws KuraClosedDeviceException, KuraGPIODeviceException {
		s_logger.info("Temp Sub has started with config!");
		//updated(properties);
		this.properties = properties;

		for (Entry<String, Object> property : properties.entrySet()) {
			s_logger.info("Update - {}: {}", property.getKey(), property.getValue());
		}

		s_logger.info("Activating Temp... Done.");
	
		//GPIO
		//KuraGPIOPin pin = gpioService.getPinByTerminal(12);
		//KuraGPIOPin customInputPin = gpioService.getPinByTerminal(26, KuraGPIODirection.INPUT,KuraGPIOMode.INPUT_PULL_UP,KuraGPIOTrigger.BOTH_LEVELS);
		this.ledRed = gpioService.getPinByTerminal(12);
		this.ledGreen = gpioService.getPinByTerminal(26);
		
		try {
			//if(this.ledRed.isOpen()==true) {
			//this.ledRed.close();
			this.ledRed.open();
			//}
			//if(this.ledGreen.isOpen()==true) {
				//this.ledGreen.close();
			this.ledGreen.open();
			//}
			//this.ledRed.open();
			//this.ledGreen.open();
			//trying to test the pin
			//int a = 0;
			//while (a<10) {
			this.ledRed.setValue(true);
			this.ledGreen.setValue(true);
				//pin.setValue(false);
				//a= a +1;
				
			//customInputPin.open();
		} catch (KuraUnavailableDeviceException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		s_logger.info("gpio has started");

	}

	protected void deactivate(ComponentContext componentContext) throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		s_logger.info("Tem Sub has stopped!");
		this.worker.shutdown();
		this.ledRed.setValue(false);
		this.ledRed.close();
		
		this.ledGreen.setValue(false);
		this.ledGreen.close();
	}

	public void updated(Map<String, Object> properties) {
		this.properties = properties;
		for (Entry<String, Object> property : properties.entrySet()) {
			s_logger.info("Update - {}: {}", property.getKey(), property.getValue());
		}
		// try to kick off a new job
		//doUpdate(true);
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

	//Method called everytime a new message arrives
	@Override
	public void onMessageArrived(KuraMessage message) {
		logReceivedMessage(message);
		// TODO Auto-generated method stub

	} 

	//private methods
	private void logReceivedMessage(KuraMessage msg) {
		KuraPayload payload = msg.getPayload();
		Date timestamp = payload.getTimestamp();
		if (timestamp != null) {
			s_logger.info("Message timestamp: {}", timestamp.getTime());
		}

		if (payload.metrics() != null) {
			String name = null; //will be 'temperature' or 'humidity'
			String status = "false";
			for (Entry<String, Object> entry : payload.metrics().entrySet()) {
				s_logger.info("Message metric: {}, value: {}", entry.getKey(), entry.getValue());
				
				if(entry.getKey().equals("name")) {
					
					name = entry.getValue().toString();
					s_logger.info("Message if print name == {}",name);
				}
				
				if(entry.getKey().equals("status")) {
					status = entry.getValue().toString();
					s_logger.info("message if print status == {}", status);
				}
			}
			if(name.equals("temperature")){
				this.tempLedStatus = Boolean.parseBoolean(status);
				s_logger.info("entering in name=temperature");
			} 
			if(name.equals("humidity")) {
				this.humLedStatus = Boolean.parseBoolean(status);
				s_logger.info("entering in name=humidity");
			}
				
			try {
				check_threshold();
			} catch (KuraUnavailableDeviceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KuraClosedDeviceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	
	public void check_threshold() throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		
		if((tempLedStatus==true)&(humLedStatus==true)) {
			this.ledRed.setValue(true);
			this.ledGreen.setValue(true);
		} else if ((tempLedStatus==true)&(humLedStatus==false)) {
			this.ledRed.setValue(true);
			this.ledGreen.setValue(false);
		} else if ((tempLedStatus==false)&(humLedStatus==true)) {
			this.ledRed.setValue(false);
			this.ledGreen.setValue(true);
		} else {
			this.ledRed.setValue(false);
			this.ledGreen.setValue(false);
		}
		
	}

}
