/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServerController;

/**
 *
 * @author Tien Thanh
 */
public class QueryIO {
    public static final int FILE_SIZE = 1024;//bytes
    
    private String name = null;
    
    private String[] parameters = null;
    
    private String result = null;
    
    private boolean hasPara = false;
    
    private byte[] byteArr = null;

    public QueryIO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
        setHasPara(true);
    }

    public String[] getParameters() {
        return parameters;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isHasPara() {
        return hasPara;
    }

    private void setHasPara(boolean hasPara) {
        this.hasPara = hasPara;
    }

    public byte[] getByteArr() {
        return byteArr;
    }

    public void setByteArr(byte[] byteArr) {
        this.byteArr = byteArr;
    }
    
    
    
}

