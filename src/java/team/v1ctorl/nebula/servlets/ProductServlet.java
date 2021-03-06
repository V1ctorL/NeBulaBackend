/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team.v1ctorl.nebula.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import team.v1ctorl.nebula.models.Product;
import team.v1ctorl.nebula.utils.DbUtil;
import team.v1ctorl.nebula.utils.SnowFlake;

/**
 *
 * @author asus
 */
@WebServlet("/product/*")
public class ProductServlet extends HttpServlet {
    DbUtil dbUtil;
    SnowFlake snowFlake;

    @Override
    public void init() throws ServletException {
        dbUtil = new DbUtil();
        snowFlake = new SnowFlake(0, 0);
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String [] splitedURI = requestURI.split("/");
        
        PrintWriter out = response.getWriter();
        
        if (splitedURI.length > 3) {
            // The request is asking for a specific product, search for it in the database.
            ResultSet rs = dbUtil.executeQuery("SELECT * FROM products WHERE id=" + splitedURI[3]);
            try {
                if (rs.next()) {
                    // Load data from ResultSet to a Java object.
                    Product product = new Product();
                    product.setId(rs.getLong("id"));
                    product.setName(rs.getString("name"));
                    product.setPrice(rs.getFloat("price"));
                    product.setAmountOfStock(rs.getInt("amount_of_stock"));
                    product.setDescription(rs.getString("description"));
                    
                    // Do serialization.
                    ObjectMapper objectMapper =  new ObjectMapper();
                    String json = objectMapper.writeValueAsString(product);
                    
                    // Return response.
                    out.println(json);
                }
                else {
                    // The requested product is not found.
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Met SQLException while searching for a product.");
            }
        }
        else {
            // The request is not asking for a specific product, return the list of all products.
            ResultSet rs = dbUtil.executeQuery("SELECT * FROM products;");
            List<Product> products = new ArrayList<>();
            try {
                while (rs.next()) {
                    // Load data from ResultSet to a Java object.
                    Product product = new Product();
                    product.setId(rs.getLong("id"));
                    product.setName(rs.getString("name"));
                    product.setPrice(rs.getFloat("price"));
                    product.setAmountOfStock(rs.getInt("amount_of_stock"));
                    product.setDescription(rs.getString("description"));
                    
                    products.add(product);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Met SQLException while loading all products.");
            }
            
            // Do serialization.
            ObjectMapper objectMapper =  new ObjectMapper();
            String json = objectMapper.writeValueAsString(products);

            // Return response.
            out.println(json);
        }
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id;
        String name;
        Float price;
        Integer amountOfStock;
        String description;
        
        String contentType = request.getContentType();
        if (contentType.equals("application/json")) {
            String json = request.getReader().readLine();
            
            // Deserialization
            ObjectMapper objectMapper = new ObjectMapper();
            Product product = objectMapper.readValue(json, Product.class);
            
            name = product.getName();
            price = product.getPrice();
            amountOfStock = product.getAmountOfStock();
            description = product.getDescription();
        }
        else if (contentType.equals("application/x-www-form-urlencoded")) {
            name = request.getParameter("name");
            price = Float.valueOf(request.getParameter("price"));
            amountOfStock = Integer.valueOf(request.getParameter("amountOfStock"));
            description = request.getParameter("description");  // The maximum length of a String object and that of TEXT in MySQL are equal, which is 65535.
        }
        else {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return;
        }
        
        PrintWriter out = response.getWriter();
        
        // Generate an ID
        id = snowFlake.nextId();
        
        if (description == null) {
            if (amountOfStock == null) 
                dbUtil.executeUpdate("INSERT INTO products (id, name, price) VALUES (" + id + ", '" + name + "', " + price + ");");
            else
                dbUtil.executeUpdate("INSERT INTO products (id, name, price, amount_of_stock) VALUES (" + id + ", '" + name + "', " + price + ", " + amountOfStock + ");");
        }
        else {
            StringBuffer sb = new StringBuffer();  // StringBuffer is thread-safe, though StringBuilder is faster.
            if (amountOfStock == null)
                sb
                        .append("INSERT INTO products (id, name, price, description) VALUES (")
                        .append(id).append(", '")
                        .append(name).append("', ")
                        .append(price).append(", '")
                        .append(description.replaceAll("'", "''"))  // Handle single quotation marks
                        .append("');");
            else
                sb
                        .append("INSERT INTO products (id, name, price, amount_of_stock, description) VALUES (")
                        .append(id).append(", '")
                        .append(name).append("', ")
                        .append(price).append(", ")
                        .append(amountOfStock).append(", '")
                        .append(description.replaceAll("'", "''"))  // Handle single quotation marks
                        .append("');");
            dbUtil.executeUpdate(sb.toString());
        }
        response.setStatus(HttpServletResponse.SC_CREATED);
    }

    /**
     * Handles the HTTP <code>PUT</code> method.
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String [] splitedURI = requestURI.split("/");
        
        PrintWriter out = response.getWriter();
        
        if (splitedURI.length > 3) {
            // The request is trying to delete a specific product, search for it in the database.
            ResultSet rs = dbUtil.executeQuery("SELECT * FROM products WHERE id=" + splitedURI[3]);
            try {
                if (rs.next()) {  // The requested product is found.
                    String json = request.getReader().readLine();

                    // Deserialization.
                    ObjectMapper objectMapper = new ObjectMapper();
                    Product product = objectMapper.readValue(json, Product.class);
                    
                    String name = product.getName();
                    Float price = product.getPrice();
                    Integer amountOfStock = product.getAmountOfStock();
                    String description = product.getDescription();
                    
                    // Handle condition that no data is provided
                    if (name==null && price==null && amountOfStock==null && description==null) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    // Update data in the database.
                    StringBuffer sb = new StringBuffer();
                    sb.append("UPDATE products SET ");
                    if (name != null)
                        sb.append("name='").append(name).append("', ");
                    if (price != null)
                        sb.append("price=").append(price).append(", ");
                    if (amountOfStock != null)
                        sb.append("amount_of_stock=").append(amountOfStock).append(", ");
                    if (description != null)
                        sb.append("description='").append(description.replaceAll("'", "''")).append("', ");  // Handle single quotation marks
                    sb.delete(sb.length()-2, sb.length()).append(" WHERE id=" + splitedURI[3]);
                    
                    dbUtil.executeUpdate(sb.toString());
                }
                else {  // The requested product is not found.
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Met SQLException while updating a product.");
            }
        }
        else {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Handles the HTTP <code>DELETE</code> method.
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String [] splitedURI = requestURI.split("/");
        
        PrintWriter out = response.getWriter();
        
        if (splitedURI.length > 3) {
            // The request is trying to delete a specific product, search for it in the database.
            ResultSet rs = dbUtil.executeQuery("SELECT * FROM products WHERE id=" + splitedURI[3]);
            try {
                if (rs.next()) {
                    // The requested product is found.
                    dbUtil.executeUpdate("DELETE FROM products WHERE id=" + splitedURI[3]);
                }
                else {
                    // The requested product is not found.
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Met SQLException while deleting a product.");
            }
        }
        else {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @Override
    public void destroy() {
        dbUtil.close();
    }

}
