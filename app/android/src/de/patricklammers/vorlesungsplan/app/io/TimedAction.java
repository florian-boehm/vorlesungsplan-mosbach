package de.patricklammers.vorlesungsplan.app.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.patricklammers.vorlesungsplan.app.interfaces.CancelableThread;

public class TimedAction extends Thread implements CancelableThread {

	private long 	waitTime;
	private Object  receiver;
	private Method 	method;
	private Object  args;
	private boolean canceled;
	private Object	result;
	
	public TimedAction(long waitTime, Object receiver, Method method, Object args) {
		this.method = method;
		this.receiver = receiver;
		this.waitTime = waitTime;
		this.args = args;
		this.canceled = false;
		this.result = null;
	}
	
	public void run() {
		try {
			TimedAction.sleep(waitTime);
			
			if(!canceled) {
				result = method.invoke(receiver, args);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}		
	}

	public Object getResult() {
		return this.result;
	}

	public boolean cancel() {
		return this.canceled = true;
	}
}
