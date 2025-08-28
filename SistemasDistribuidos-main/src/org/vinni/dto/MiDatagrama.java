package org.vinni.dto;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiDatagrama {
    public static DatagramPacket crearDataG(String ip, int puerto, String mensaje){
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            //UTF8
            byte[] mensajeB = mensaje.getBytes(StandardCharsets.UTF_8);
            return new DatagramPacket(mensajeB, mensajeB.length, direccion, puerto);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MiDatagrama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
