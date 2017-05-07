/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tien Thanh
 */
public class ServerService {
    
    private ArrayList<QueryIO> queryObjs = new ArrayList();
    
    private File f = null;
    
    private String childName = "he";

    public ServerService() {        
        try {
            //Set file currsor cho thread
            this.f = new File(".\\server-data");
            this.f = new File(this.f.getCanonicalPath());
            
        } catch (IOException ex) {
            Logger.getLogger(ServerService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void setResult(){
        for(QueryIO queryobj : this.queryObjs){
            String result = "";
            switch (queryobj.getName()){
                case "getdate"  : 
                    result = "" + new Date();
                    queryobj.setResult(result);
                    break;

                case "help"   : 
                    result = getAllRequirement();
                    queryobj.setResult(result);
                    break;

                case "getpath"   : 
                    result = " ***GET_CURRENT_PATHFILE " + getCurrentPath();
                    queryobj.setResult(result);
                    break;
                    
                case "removepath" :
                    result = " ***REMOVE_CURRENT_PATHFILE ";
                    queryobj.setResult(result);
                    break;

                case "cd.." : 
                    result = " ***GET_CURRENT_PATHFILE " + getParentPath();
                    queryobj.setResult(result);
                    break;
                    
                case "moveto" : 
                    try {
                        String childName = queryobj.getParameters()[0];
                        if(moveToChild(childName) == true){
                            result = " ***GET_CURRENT_PATHFILE " + getCurrentPath();
                        } else {
                            result = "Không có thư mục "+childName+" hoặc xảy ra lỗi trên đường truyền";
                        }
                        
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh moveto";
                    }
                    
                    queryobj.setResult(result);
                    break;
                
                case "mkdir" :
                    try {
                        String dirName = queryobj.getParameters()[0];
                        if(makeDirectory(dirName) == true){
                            result = "Tạo folder thành công";
                        } else {
                            result = "Folder đang tồn tại hoặc tên không hợp lệ";
                        }
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh mkdir";
                    }
                    
                    queryobj.setResult(result);
                    break;
                    
                case "mkfile" :
                    try {
                        String fileName = queryobj.getParameters()[0];
                        if(makeFile(fileName) == true){
                            result = "Tạo file thành công";
                        } else {
                            result = "File đang tồn tại hoặc tên không hợp lệ";
                        }
                        
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh mkfile";
                    }
                    
                    queryobj.setResult(result);
                    break;
                    
                case "ls"   :   
                    result = "" + getChildNames();
                    queryobj.setResult(result);
                    break;
                    
                case "delete" :
                    try {
                        String name = queryobj.getParameters()[0];
                        if(delete(name) == true){
                            result = "Xóa thành công";
                        } else {
                            result ="Xóa thất bại, tên không tồn tại hoặc không hợp lệ";
                        }
                        
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh delete";
                    }
                    
                    
                    queryobj.setResult(result);
                    break;
                    
                case "cut" :
                    try {
                        String oldPath = queryobj.getParameters()[0];
                        String newPath = queryobj.getParameters()[1];

                        if(cut(oldPath, newPath) == true){
                            result = "Di chuyển thành công";
                        } else {
                            result = "Có lỗi khi di chuyển, vui lòng xem lại đường dẫn hoặc gõ help để xem cách dùng lệnh cut";
                        }
                        
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh cut";
                    }
                    
                    queryobj.setResult(result);
                    break;
                    
                case "get" ://Trả về lệnh nhận file và độ dài file (bytes)
                    try {
                        String nameOfFileToSend = queryobj.getParameters()[0];
                        
                        byte[] byteArr = getFile(nameOfFileToSend);
                        
                        if(byteArr.length > 0){
                            queryobj.setByteArr(byteArr);
                            
                            if(queryobj.getParameters().length == 1){
                                queryobj.setResult(" ***RECEIVE_FILE $" + byteArr.length +"$"+ nameOfFileToSend);
                            } else {
                                queryobj.setResult(" ***RECEIVE_FILE $" + byteArr.length +"$" + nameOfFileToSend +"$HANDLE");
                            }

                        } else {
                            queryobj.setResult("File rỗng");
                        }
                        
                    } catch (NullPointerException e) {
                        result = "Lệnh không hợp lệ, gõ help để xem cách dùng lệnh get";
                        queryobj.setResult(result);
                    }
                    
                    break;
                
            }
        }
    }
    
    
    
    public String getAllRequirement(){
        return "help: In danh sách lệnh \r\n"
                + "getdate: Hiển thị thời gian hệ thống \r\n"
                + "getpath: Hiển thị thư mục \r\n"
                + "removepath: Bỏ hiển thị thư mục\r\n"
                + "ls: In danh sách tệp và thư mục trong thư mục hiện tại, tên tệp và thư mục sẽ đặt trong cặp [ và ] \r\n"
                + "cd..: Đến thư mục cha của thư mục hiện tại \r\n"
                + "moveto([folder-name]): Di chuyển đến thư mục truyền vào \r\n"
                + "mkdir([folder-name]): Tạo thư mục\r\n"
                + "mkfile([file-name]): Tạo tệp\r\n"
                + "delete([name]): Xóa tệp hoặc thư mục\r\n"
                + "cut([old-path],[new-path]): Di chuyển và đổi tên thư mục hoặc tệp\r\n"
                + "get([file-name]): Lấy tệp từ Server\r\n"
                + "get([file-name],[speed]): Lấy tệp từ Server với tốc độ giới hạn";
    }

    
    public String getCurrentPath(){    
        return this.f.getAbsolutePath();
    }
    
    public String getParentPath() {
        if(this.f.getParent() != null){
            this.f = new File(this.f.getParent());
        }
        
        
        return this.f.getAbsolutePath();
    }
    
    public String getChildNames() {
        String names = "";
        
        String[] nameArr = this.f.list();
        
        for(String name : nameArr) {
            names += "[" +name + "]";
        }
        
        return names;
    }
    
    public String moveFileCursor(String path) {
        
        
        return "moveFileCursor";
    }
    
    public boolean moveToChild(String dirChildName){
        File child_f = new File(this.f.getAbsolutePath()+"//"+dirChildName);
        
        if(child_f.exists() && child_f.isDirectory()){
            String path_child_f = child_f.getAbsolutePath();
            
            System.out.println(path_child_f);
            
            this.f = new File(path_child_f);
            
            return true;
        } else {
            return false;
        }
    }
    
    public boolean makeDirectory(String dirName){
        File f_to_create = new File(this.f.getAbsolutePath() + "//"+ dirName);
        if(f_to_create.exists() == true){
            return false;
        } else {
            return f_to_create.mkdir();
        }
    }
    
    public boolean makeFile(String fileName){
        File f_to_create = new File(this.f.getAbsolutePath() + "//"+ fileName);
        if(f_to_create.exists() == true){
            return false;
        } else {
            try {
                return f_to_create.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ServerService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return false;
    }
    
    public boolean cut(String oldPath, String newPath){
        File f_old = new File(this.f.getAbsolutePath() + "//"+oldPath);
        File f_new = new File(this.f.getAbsolutePath() + "//"+newPath);
        
        if(f_old.exists() == true){//Nếu địa chỉ cũ tồn tại
            if(f_new.exists() == false){//Nếu địa chỉ mới chưa tồn tại
                return f_old.renameTo(f_new);//thì tiến hành cut
            } else {//Nếu địa chỉ mới tồn tại
                return false;//thì không tiến hành
            }
            
        } else {//Nếu địa chỉ cũ không tồn tại
            return false;//thì không tiến hành
        }
        
    }
    
    public boolean delete(String name){
        File f_to_del = new File(this.f.getAbsolutePath() + "//"+ name);
        if(f_to_del.exists() == true){
            return f_to_del.delete();
        } else {
            return false;
            
        }
        
    }
    
    public byte[] getFile(String fileName){
        String filePath = this.f.getAbsolutePath() + "//"+fileName;
        File f_to_send = new File(filePath);
        
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        
        try {  
            if(f_to_send.exists() && f_to_send.isFile()){
                int fileSize = (int) f_to_send.length();
                byte[] byteArr = new byte[fileSize];
                fis= new FileInputStream(f_to_send);
                bis = new BufferedInputStream(fis);
                bis.read(byteArr, 0, fileSize);
                return byteArr;

            }
        } catch (FileNotFoundException ex) {
                Logger.getLogger(ServerService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(fis != null){
                    fis.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ServerService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return null;
    }

    public void setQueryObjs(ArrayList<QueryIO> queryObjs) {
        this.queryObjs = queryObjs;
    }
    
    
    
    
}
