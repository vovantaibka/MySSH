/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import ServerController.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Tien Thanh
 */
public class Server {
    
    public static ArrayList listServiceThread = new ArrayList();
    public static final int MAX_CLIENDS = 3;
    
    public static void main(String[] args) throws IOException {
        
        Server sv = new Server();
        
        
        ServerSocket listener = null;
        
        System.out.println("Watting for connection...");
        
        int clientConnected = 0;
        
        //Open a SeverSocket in SEVER_PORT
        try {
            listener = new ServerSocket(ServerController.Configs.SEVER_PORT);
            
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            while (true) {                
                //Chờ và chấp nhận kết nối từ client
                //đồng thời nhận 1 đối tượng Socket tại server
                Socket socketOfServer = listener.accept();
                
                //tạo 1 thread để giao tiếp với client đó
                ServiceThread serviceThread = new ServiceThread(socketOfServer);
                
                serviceThread.start();
                
                listServiceThread.add(serviceThread);
                
                System.out.println("Client kết nối " + (listServiceThread.indexOf(serviceThread) + 1));
                
                if(listServiceThread.indexOf(serviceThread) > ServerController.Configs.MAX_CLIENT_CONNECTED - 1){//Check connected fully
                    serviceThread.sendError("Server quá bận, vui lòng kết nối lại sau");
                    
                    serviceThread.closeConnected();
                    
                };
 
            }
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            listener.close();
        }
        
    }
    
    
    //Khai báo một nested class là thread giao tiếp với client
    private static class ServiceThread extends Thread {
        
        public int indexOfList;
        private Socket socketOfServer;
        private int userId;
        private boolean isLogged = false;
        private boolean isContinue = true;
        private ServerService serverService = null;
        
        private BufferedReader is;
        private BufferedWriter os;
        
        public ServiceThread(Socket socketOfServer){
            this.socketOfServer = socketOfServer;
            
            serverService = new ServerService();
        }
        
        @Override
        public void run(){
            try {
                getListInform();
                //mở luồng vào ra trên Socket tại Server
                is = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
                
                System.out.println("Name: "+socketOfServer.getInetAddress().getHostName());
                System.out.println("IP: "+socketOfServer.getInetAddress().getHostAddress());
                //Gui yeu cau dang nhap
                os.write(" Ban can dang nhap vao he thong, cu phap: taikhoan matkhau");
                os.newLine();
                os.flush();
                
                //Login loop
                while(isLogged == false) {
                    requireLogin();
                }
                
                //Communication loop
                while (isContinue) {
                    if(is.ready()){
                        this.isContinue = false;
                    }
                    String line = is.readLine();
                    
                    //Validate day lenh
                    line = line.trim();
//                    line = line.replaceAll("\\s+", " ");
                    
                    // Nếu người dùng gửi tới QUIT (Muốn kết thúc trò chuyện).
                    if (line.equals("QUIT")) {
                        closeConnected();
                        
                    }
                    
                    if(line.indexOf("***CONTINUE") != -1){
                        continue;
                    }
                    
                    String[] queries = line.split("\\$");
                                     
                    ArrayList<QueryIO> queryObjs = new ArrayList();
                    //Chuyển String querry thành querryObject và cho vào arraylist
                    for(String query : queries){
                        //Valiate query
                        query = query.trim();
                        
                        QueryIO queryObj = new QueryIO();
                        
                        if(query.indexOf("(") == -1 && query.indexOf(")") == -1){//Nếu là lệnh không có tham số
                            queryObj.setName(query);
                        } else if(query.indexOf("(") != -1 && query.indexOf(")") != -1){//Nếu là lệnh có tham số
                            String[] st = query.split("\\(|\\)|,");//Chia các thành phần của lệnh, trả về [tên, para1, para2,..]
                            
                            queryObj.setName(st[0]);
                            
                            if(st.length > 1){
                                String[] para = new String[st.length -1];
                                
                                for(int i =1; i<st.length; i++){
                                    para[i-1] = st[i];
                                }
                                
                                queryObj.setParameters(para);
                                System.out.println(para[0]);
                            }
                            
                            
                        } else {//Nếu có lệnh sai cú pháp thì break
                            queryObjs.clear();
                            break;
                        }
                        
                        queryObjs.add(queryObj);
                    }
                    
                    //Nếu list querry không rỗng thì tiến hành các lệnh
                    //result của lệnh sẽ lưu vào thuộc tính của queryObj
                    if(queryObjs.size() > 0){
                        //Đưa danh sách lệnh
                        serverService.setQueryObjs(queryObjs);
                        //Thực hiện lệnh và lưu kết quả trả về
                        serverService.setResult();
                        //Đưa kết quả trả về cho client
                        for(QueryIO querryObj : queryObjs){
                            String result = querryObj.getResult();
                            if(result != null){
                                sendMessage(result);
                            } else {
                                sendError("Có lỗi khi thực hiện lệnh !!!");
                            }
                            
                            if(querryObj.getByteArr() != null){
                                byte[] byteArr = querryObj.getByteArr();
                                if(querryObj.getParameters().length == 1){
                                    sendByteArr(byteArr);
                                } else if(querryObj.getParameters().length == 2){
                                    sendByteArrWithMaxSpeed(byteArr, Integer.parseInt(querryObj.getParameters()[1]));
                                }
                                
                            }
                        }
                    } else {
                        sendError("Có lỗi khi thực hiện lệnh !!!");
                    }
                    
                }
                
                is.close();
                os.close();
                this.socketOfServer.close();
                
            }catch(java.net.SocketException ex) {
                System.out.println("Client mat ket noi");
                closeForce();
                
            }catch (IOException ex) {
                System.out.println("Client mat ket noi");
                closeForce();
            }
            
        }
        
        public void sendByteArr(byte[] byteArr){
            try {
                //Tạo một luồng mới để gửi dữ liệu dạng byte[]
                OutputStream oos = socketOfServer.getOutputStream();
                oos.write(byteArr, 0, byteArr.length);
                oos.flush();
                
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void sendByteArrWithMaxSpeed(byte[] byteArr, int maxByte) {
            OutputStream oos = null;
            System.out.println("Số bytes tối đa gửi mỗi lần: "+maxByte);
            try {
                oos = socketOfServer.getOutputStream();
                int fileSize = byteArr.length;
                int bytesNeedToSend = fileSize;
                int current = 0;
                int bytesSend = 0;
                
                do{
                    if(maxByte > bytesNeedToSend) bytesSend = bytesNeedToSend;
                    else bytesSend = maxByte;
                    
                    oos.write(byteArr, current, bytesSend);
                    oos.flush();
                    
                    bytesNeedToSend = bytesNeedToSend - bytesSend;
                    current += bytesSend;
                    
                } while(bytesNeedToSend > 0);
                
                System.out.println("Da gui xong!!");
                
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public void sendMessage(String messStr) {
            try {
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
                os.write("" + messStr);
                os.newLine();
                os.flush();
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        public void sendError(String errStr) {
            try {
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
                os.write("ERROR: " + errStr);
                os.newLine();
                os.flush();
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        public void requireLogin(){
            boolean loginSuccess = false;
            try {
                String line = is.readLine();
                
                String[] loginInform = line.split(" ");
                
                if(loginInform.length < 2){
                    sendError("Vui long nhap dung cu phap TaiKhoan[KhoangTrang]MatKhau.");
                    return;
                }
                
                String taiKhoan = loginInform[0];
                
                String matKhau = loginInform[1];
                
                if(taiKhoan == null){
                    sendError(" Thieu tai khoan");
                    return;
                } else if(matKhau == null){
                    sendError(" Thieu mat khau");
                    return;
                } else {
                    ArrayList lstInform = getListInform();
                    loginSuccess = confirmInform(taiKhoan, matKhau, lstInform);
                }
                
                if(loginSuccess == true){
                    isLogged = true;
                    os.write(" Đăng nhập thành công, nhập help để xem danh sách lệnh.");
                    os.newLine();
                    os.flush();
                } else {
                    sendError(" Dang nhap that bai");
                }
                  
            } catch(java.net.SocketException e){
                System.out.println("Client mất kết nối");
                closeForce();
                return;
            } catch (NullPointerException e) {
                System.out.println("Client mất kết nối!");
                closeForce();
                return;
            } catch (IOException ex){
                System.out.println("Client mất kết nối");
                closeForce();
                return;
            } 
            
        }
        
        public boolean confirmInform(String userName, String pass, ArrayList<ArrayList> lstInform){
            boolean confirmed = false;
            try {
                for(ArrayList informs : lstInform){
                    if(userName.compareTo((informs.get(0).toString())) == 0){//neu userName chiinh xac
                        if(pass.compareTo((informs.get(1)).toString()) != 0){//ma pass ko chih xac thi return false
                            os.write(">> Mat khau khong chinh xac");
                            os.newLine();
                            os.flush();
                            return confirmed = false;
                        } else{//con neu pass chinh xac thi return true
                            return confirmed = true;
                        }
                    }
                }
                //Neu khong co userName nao chinh xac
                os.write(">> Khong co "+userName + " trong he thong.");
                os.newLine();
                os.flush();
            } catch (Exception ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return confirmed;
        }
        
        public ArrayList<ArrayList> getListInform(){
            ArrayList<ArrayList> listInform = new ArrayList();//Chua thong tin tat ca users
            ArrayList<String> lst = null;//chua thong tin tung user
            
            try {
                InputStream in = new FileInputStream("UserInformation/user-information.txt");
                Reader reader = new InputStreamReader(in, "UTF-8");
                BufferedReader br = new BufferedReader(reader);
                
                String lineStr = null;
                while((lineStr = br.readLine()) != null){
                    lst = new ArrayList();
                    for(String inform : lineStr.split(" ")){
                        lst.add(inform);
                    }
                    
                    listInform.add(lst);
                    
                }
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            return listInform;
        }
        
        public boolean closeConnected(){
            try {
                os = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
                //res cho client biet la sever se close ket noi nay
                os.write(">> ***EXIT");
                os.newLine();
                os.flush();
                
                isContinue = false;
                    
                System.out.println("Disconected client");
                
                removeThisThread();
                
                return true;
                
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return false;
            
        }
        
        public boolean closeForce(){
            removeThisThread();
            this.stop();
            return true;
        }
        
        public void removeThisThread(){
            listServiceThread.remove(this);
        }
        
    }
    
    
    
    
    
}
