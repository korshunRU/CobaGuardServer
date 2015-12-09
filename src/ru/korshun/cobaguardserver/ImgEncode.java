/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.korshun.cobaguardserver;

import java.io.*;

/**
 *
 * @author user
 */
public class ImgEncode {

    private String                  imgInputPath,
                                    imgOutputPath,
                                    imgName;

    private ImgEncode(String imgInputPath, String imgOutputPath) {

        this.imgInputPath =                                         imgInputPath;
        this.imgOutputPath =                                        imgOutputPath;

    }

    private ImgEncode(String imgName, String imgInputPath, String imgOutputPath) {

        this.imgName =                                              imgName;
        this.imgInputPath =                                         imgInputPath;
        this.imgOutputPath =                                        imgOutputPath;

    }


    public static ImgEncode getInstance(String imgInputPath, String imgOutputPath) {
        return new ImgEncode(imgInputPath, imgOutputPath);
    }


    public static ImgEncode getInstance(String imgName, String imgInputPath, String imgOutputPath) {
        return new ImgEncode(imgName, imgInputPath, imgOutputPath);
    }


    // Функция проверки существования директории
    private static void dirExists(String dir) {

        if(!new File(dir).exists()) {
            new File(dir).mkdir();
        }

    }



    protected void encodeDir() {

        dirExists(imgOutputPath);

        for(File f :                                                new File(imgInputPath).listFiles()) {

            if(f.isFile()) {

                byte[] enc =                                        load(f.getName());
                String e =                                          encode(enc);

                File f_write =                                      new File(imgOutputPath + File.separator + f.getName());

                    try (BufferedWriter output =                    new BufferedWriter(new FileWriter(f_write))) {
                        output.write(e);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

            }

        }

    }


    protected void encodeImg() {

        dirExists(imgOutputPath);

        File file =                                                 new File(imgInputPath + File.separator + imgName);

        if(file.isFile()) {

            byte[] enc =                                            load(file.getName());
            String e =                                              encode(enc);

            File f_write =                                          new File(imgOutputPath + File.separator + file.getName());

            try (BufferedWriter output =                            new BufferedWriter(new FileWriter(f_write))) {
                output.write(e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

    }



    private byte[] load(String fileName) {

        try(InputStream in =                                        new FileInputStream(imgInputPath + File.separator + fileName)) {

            ByteArrayOutputStream bout =                            new ByteArrayOutputStream();
            byte[] buffer =                                         new byte[32 * 1024];

            while (true) {
                int r =                                             in.read(buffer);
                if (r > 0) {
                    bout.write(buffer, 0, r);
                }
                else {
                    break;
                }
            }

            bout.close();

            return bout.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    private String encode(byte[] d) {
           if (d == null) return null;
       byte data[] =                                                new byte[d.length+2];
       System.arraycopy(d, 0, data, 0, d.length);
       byte dest[] =                                                new byte[(data.length/3)*4];

           // 3-byte to 4-byte conversion
           for (int sidx = 0, didx=0; sidx < d.length; sidx += 3, didx += 4) {
               dest[didx]   = (byte) ((data[sidx] >>> 2) & 077);
               dest[didx+1] = (byte) ((data[sidx+1] >>> 4) & 017 | (data[sidx] << 4) & 077);
               dest[didx+2] = (byte) ((data[sidx+2] >>> 6) & 003 | (data[sidx+1] << 2) & 077);
               dest[didx+3] = (byte) (data[sidx+2] & 077);
           }

           for (int idx = 0; idx < dest.length; idx++) {
               if (dest[idx] < 26)         { dest[idx] = (byte)(dest[idx] + 'A'); }
               else if (dest[idx] < 52)    { dest[idx] = (byte)(dest[idx] + 'a' - 26); }
               else if (dest[idx] < 62)    { dest[idx] = (byte)(dest[idx] + '0' - 52); }
               else if (dest[idx] < 63)    { dest[idx] = (byte)'+'; }
               else                        { dest[idx] = (byte)'/'; }
           }

           for (int idx = dest.length-1; idx > (d.length*4)/3; idx--) { dest[idx] = (byte)'='; }

       return new String(dest);

  }

      private String encode(String s) { return encode(s.getBytes()); }

}
