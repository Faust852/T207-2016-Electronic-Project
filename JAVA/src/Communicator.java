/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import gnu.io.*;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;

import javax.swing.JOptionPane;

public class Communicator implements SerialPortEventListener
{
	//create a new interface
    GUI window = null;

    //for containing the ports that will be found
    private Enumeration ports = null;
    //map the port names to CommPortIdentifiers
    private HashMap portMap = new HashMap();

    //this is the object that contains the opened port
    private CommPortIdentifier selectedPortIdentifier = null;
    private SerialPort serialPort = null;

    //input and output streams for sending and receiving data
    private InputStream input = null;
    private OutputStream output = null;

    //flag to enable button if connected to a serial port, keep them disable if not
    private boolean bConnected = false;

    //the timeout value for connecting with the port
    final static int TIMEOUT = 2000;

    //some ascii values for for certain things
    final static int SPACE_ASCII = 32;
    final static int DASH_ASCII = 45;
    final static int NEW_LINE_ASCII = 10;
    //double dot used as first byte char send after a treshold value, to be interpreted by the PIC Code
    final static int DOUBLE_DOT = 48;

    //a string for recording what goes on in the program
    //this string is written to the GUI
    String logText = "";

    public Communicator(GUI window)
    {
        this.window = window;
    }

    /**
     * Method used to look for ports, and showing them in a combobox in the GUI
     */
    public void searchForPorts()
    {
        ports = CommPortIdentifier.getPortIdentifiers();

        while (ports.hasMoreElements())
        {
            CommPortIdentifier curPort = (CommPortIdentifier)ports.nextElement();

            //get only serial ports
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL)
            {
                window.cboxPorts.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
            }
        }
    }

    /**
     * Method to connect the app to the serial ports
     * At leat one com port must be available
     */
    public void connect()
    {
        String selectedPort = (String)window.cboxPorts.getSelectedItem();
        selectedPortIdentifier = (CommPortIdentifier)portMap.get(selectedPort);

        CommPort commPort = null;

        try
        {
            //the method below returns an object of type CommPort
            commPort = selectedPortIdentifier.open("TigerControlPanel", TIMEOUT);
            //the CommPort object can be casted to a SerialPort object
            serialPort = (SerialPort)commPort;

            //for controlling GUI elements
            setConnected(true);

            //logging
            logText = selectedPort + " opened successfully.";
            window.txtLog.setForeground(Color.black);
            window.txtLog.append(logText + "\n");

            //Baud and stuff should have already been choosed with VPSE

            //enables the controls on the GUI if a successful connection is made
            window.keybindingController.toggleControls();
        }
        catch (PortInUseException e)
        {
            logText = selectedPort + " is in use. (" + e.toString() + ")";
            
            window.txtLog.setForeground(Color.RED);
            window.txtLog.append(logText + "\n");
        }
        catch (Exception e)
        {
            logText = "Failed to open " + selectedPort + "(" + e.toString() + ")";
            window.txtLog.append(logText + "\n");
            window.txtLog.setForeground(Color.RED);
        }
    }

    /**
     * initiate the byte stream
     */
    public boolean initIOStream()
    {
        //return value for whather opening the streams is successful or not
        boolean successful = false;

        try {
            //
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
            writeData(400); //400 is the default value chosen
            
            successful = true;
            return successful;
        }
        catch (IOException e) {
            logText = "I/O Streams failed to open. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
            return successful;
        }
    }

    /**
     * initialize the listener so that the app can react to incoming data from the PIC
     */
    public void initListener()
    {
        try
        {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        }
        catch (TooManyListenersException e)
        {
            logText = "Too many listeners. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }

	/**
	 * close correctly the connection so that a serial port doesn't get stuck
	 */
    public void disconnect()
    {
        //close the serial port
        try
        {
            writeData(400); // reput the initial value to 400, should be removed ?

            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            output.close();
            setConnected(false);
            window.keybindingController.toggleControls();

            logText = "Disconnected.";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
        catch (Exception e)
        {
            logText = "Failed to close " + serialPort.getName() + "(" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }

    final public boolean getConnected()
    {
        return bConnected;
    }

    public void setConnected(boolean bConnected)
    {
        this.bConnected = bConnected;
    }

    /**
     * method that write on the textarea when data is recieved from the pic
     */
    public void serialEvent(SerialPortEvent evt) {
    	String distanceStr = "0";
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE)
        {
            try
            {
            	byte singleData = (byte)input.read();

                if (singleData != NEW_LINE_ASCII)
                {
                    logText = new String(new byte[] {singleData});
                    distanceStr += logText;
                    window.txtLog.append(logText);
                }
                else
                {
                	float distanceF = Float.parseFloat(distanceStr);
                    float limitF = Float.parseFloat(window.lblLimit.getText());
                    System.out.println(distanceF);
                    if (limitF > distanceF) {window.lblLimitRespected.setForeground(Color.RED);
                    	window.lblLimitRespected.setText("Too close !");}
                    else {window.lblLimitRespected.setForeground(Color.GREEN);
                    window.lblLimitRespected.setText("Limit Respected");}
                    window.txtLog.append("\n");
                    distanceStr = "0";
                    
                }
            }
            catch (Exception e)
            {
                logText = "Failed to read data. (" + e.toString() + ")";
                window.txtLog.setForeground(Color.red);
                window.txtLog.append(logText + "\n");
            }
        }
    }

	/**
	 * method that send info to the PIC
	 * @param leftThrottle
	 */
    public void writeData(int threshold)
    {
        try
        {
        	byte c = DOUBLE_DOT;
			output.write((char)c);
            output.write(threshold);
            System.out.println(threshold);
            output.flush();
        }
        catch (Exception e)
        {
            logText = "Failed to write data. (" + e.toString() + ")";
            window.txtLog.setForeground(Color.red);
            window.txtLog.append(logText + "\n");
        }
    }
}

