/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ClientController;

import java.io.*;
import java.net.*;
import java.util.Date;
import ServerController.Configs;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tien Thanh
 */
public class Client extends Thread{
    
    public static String serverHost = "localhost";
    public static int portNum = 1996;
    
    Socket socketOfClient = null;
    BufferedWriter os = null;
    BufferedReader is = null;
    
    BufferedReader request = null;
    Scanner sc = null;
    String pathOfFileServer = "";
    File file_save = null;
    
    boolean isReqMore = true;
    
    public void createSocketOfClient(String serverHost, int portNum) {
        try {
            request = new BufferedReader(new InputStreamReader(System.in));
                           
            System.out.print("Nhập địa chỉ ip của Server: ");
                
            String req = request.readLine();
                
            req.trim();
            
                
            this.socketOfClient = new Socket(InetAddress.getByAddress(getIp(req)), portNum);
            
            System.out.println("Kết nối thành công.");
            
        } catch (IOException ex) {
            createSocketOfClient(serverHost, portNum);
        }
        
    }
    
    public void setFileServerPath(String serRes){
        if(serRes != null){
            Object[] res = serRes.split(" ");
            int len = res.length;
            
            String path = res[len-1].toString();
            
            this.pathOfFileServer = path;
            
            System.out.print(this.pathOfFileServer + " ");
        }

    }
    
    public void getFile(int fileSize, String fileName){
        System.out.println("Đang tải, bộ nhớ cần > "+fileSize +" bytes");
        byte [] mybytearray  = new byte [fileSize];
        try {
            InputStream iis = socketOfClient.getInputStream();
            
            this.file_save = new File(".\\client-data\\"+fileName);
            
            this.file_save = new File(this.file_save.getCanonicalPath());
            
            FileOutputStream fos = new FileOutputStream(file_save);
            
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead = -1;
            int totalBytesRead = 0;
            int current = 0;
            
            while((bytesRead = iis.read(mybytearray)) != -1){
                System.out.println("Đang tải file ...");
                
                bos.write(mybytearray, 0, bytesRead);
                totalBytesRead += bytesRead;
                System.out.println("So bytes nhan dc: " +bytesRead);
                System.out.println("Số bytes đã tải: "+totalBytesRead);
                if(totalBytesRead >= fileSize){
                    break;
                }
            }
            
            System.out.println("Tải thành công");
            
            if(bos != null) bos.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void getFileWithHandle(int fileSize, String fileName){
        byte [] mybytearray  = new byte [fileSize];
        try {
            InputStream iis = socketOfClient.getInputStream();
            
            this.file_save = new File(".\\"+fileName);
            
            this.file_save = new File(this.file_save.getCanonicalPath());
            
            FileOutputStream fos = new FileOutputStream(file_save);
            
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            
            int bytesRead = -1;
            int totalBytesRead = 0;
            int current = 0;
            
            while((bytesRead = iis.read(mybytearray, totalBytesRead, mybytearray.length - totalBytesRead)) != -1){
                System.out.println("Đang tải file ...");
                
                totalBytesRead += bytesRead;
                System.out.println("So bytes nhan dc: " +bytesRead);
                System.out.println("Số bytes đã tải: "+totalBytesRead);
                
                if(totalBytesRead >= fileSize){
                    break;
                }
                
            }
            bos.write(mybytearray, 0, fileSize);
            iis = null;
            System.out.println("Tải thành công");
            
            if(bos != null) bos.close();
            
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void listenRequest() {
        
        try {
            // Tạo luồng đầu ra tại client (Gửi dữ liệu tới server)
            os = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + serverHost);
            return;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + serverHost);
            return;
        }

        try {
            
            request = new BufferedReader(new InputStreamReader(System.in));
            
            //nghe request từ console
            while (isReqMore == true) {
                
                
                String req = request.readLine();
                
                if(req != null) {
                    // Ghi dữ liệu vào luồng đầu ra của Socket tại Client.
                    os.write(req);
                    
                    os.newLine();// kết thúc dòng
                    
                    os.flush();// đẩy dữ liệu đi.
                    
                }

            }
            
            //Đóng client
            os.close();
            is.close();
            socketOfClient.close();
             
        } catch (UnknownHostException e) {
            System.err.println("Trying to connect to unknown host: " + e);
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
        
    }
    
    @Override
    public void run(){
        try {
            // Luồng đầu vào tại Client (Nhận dữ liệu từ server).
            is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
            
            String responseLine;
                
            while (isReqMore) {    
                responseLine = is.readLine();
                
                if (responseLine!= null && responseLine.indexOf("***EXIT") != -1) {
                    System.out.println("Disconected to Server, Press Enter to exit");
                    
                    isReqMore = false;
                    
                    break;
                    
                }
                
                if (responseLine != null && responseLine.indexOf("***GET_CURRENT_PATHFILE") != -1) {
                    setFileServerPath(responseLine);
                    continue;
                }
                
                if (responseLine != null && responseLine.indexOf("***REMOVE_CURRENT_PATHFILE") != -1) {
                    this.pathOfFileServer = "";
                    continue;
                }
                
                //***RECEIVE_FILE$[file-size]$[file-name]$[[option]]
                if(responseLine != null && responseLine.indexOf("***RECEIVE_FILE") != -1) {
                    responseLine = responseLine.trim();
                    
                    String[] resParas = responseLine.split("\\$"); //return arr = [***RECEIVE_FILE,file_size,file_name]
                    int fileSize = Integer.parseInt(resParas[1]);
                    String fileName = resParas[2];
                    
                    if(resParas.length == 3){//No [option]
                        getFile(fileSize, fileName);
                        this.is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
                    } else if(resParas.length == 4){ //Has [option]
                        getFileWithHandle(fileSize, fileName);
                        this.is = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
                    }
                    
                    continue;
                }
                
                System.out.println("Server: >>> " + responseLine);
                
                //In duong dan cursor file cua server (sau khi da getCurrPath)
                System.out.print(this.pathOfFileServer + " ");
 
            }

        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static byte[] getIp(String ip) {
        String[] ipArr = ip.split("\\.");
        byte[] ipByte = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipByte[i] = (byte)Integer.parseInt(ipArr[i]);
        }
        return ipByte;
    }

    public static void main(String[] args) {
        Client client = new Client();
        
        client.createSocketOfClient(serverHost, portNum);
        
        //nghe res từ server
        client.start();
        
        //nghe req từ client này
        client.listenRequest();
        
    }
    
}
