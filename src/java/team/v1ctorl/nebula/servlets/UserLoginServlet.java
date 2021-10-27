/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team.v1ctorl.nebula.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import team.v1ctorl.nebula.utils.BCrypt;
import team.v1ctorl.nebula.utils.DbUtil;

/**
 *
 * @author asus
 */
@WebServlet("/login")
public class UserLoginServlet extends HttpServlet {
    DbUtil dbUtil;

    @Override
    public void init() throws ServletException {
        dbUtil = new DbUtil();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: return login html page
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = new String(request.getParameter("username").getBytes("ISO8859-1"), "UTF-8"); // handle Chinese
        String password = request.getParameter("password");
        
        PrintWriter out = response.getWriter();
        
        ResultSet rs = dbUtil.executeQuery("SELECT password FROM users WHERE username='" + username + "';");
        try {
            if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                out.println("approved");
            }
            else {
                out.println("denied");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Met SQLException while user logging in.");
        }
    }

    @Override
    public void destroy() {
        dbUtil.close();
    }

}