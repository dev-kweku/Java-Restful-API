package com.userapi.repository;

import com.userapi.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private final DataSource ds;

    public UserRepository(DataSource ds){
        this.ds=ds;
    }

//List all data
    public List<User> findAll(int page,int size,String country) throws SQLException{
        String sql="SELECT * ALL FROM users"
                   + (country != null ? "WHERE country = ?" :"")
                   + " ORDER BY id LIMIT ? OFFSET ?";

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            int idx=1;
            if(country != null) ps.setString(idx++,country);
            ps.setInt(idx,page * size);
            return mapResultSet(ps.executeQuery());
        }
    }

    public long count(String country) throws SQLException{
        String sql="SELECT COUNT(*) FROM users" + (country != null ? "WHERE country =?" : "");

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            if(country != null) ps.setString(1,country);
            ResultSet rs=ps.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        }
    }


    public Optional<User> findById(long id) throws SQLException{
        String sql="SELECT * FROM users WHERE id=?";
        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setLong(1,id);
            List<User> list=mapResultSet(ps.executeQuery());
            return list.isEmpty() ? Optional.empty() :Optional.of(list.getFirst());
        }

    }

    public User create(User u) throws SQLException{
        String sql= """
                INSERT INTO users (first_name,last_name,email,city,country,age,gender)
                VALUES (?,?,?,?,?,?,?) RETURNING *
                """;
        try(Connection c= ds.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            setUserParams(ps,u);
            return  mapResultSet(ps.executeQuery()).getFirst();

        }
    }
    public Optional<User> update(long id,User u)throws SQLException{
        String sql= """
                UPDATE users SET first_name=?,last_name=?,email=?,phone=?,
                city=?,country=?,gender=? WHERE id=? RETURNING *
                """;

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            setUserParams(ps,u);
            ps.setLong(9,id);
            List<User> list=mapResultSet(ps.executeQuery());
            return list.isEmpty() ? Optional.empty() :Optional.of(list.getFirst());
        }
    }

    public boolean delete(long id) throws SQLException{
        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM users WHERE id=?")){
            ps.setLong(1,id);
            return ps.executeUpdate() > 0;
        }
    }

//    helpers
    private void setUserParams(PreparedStatement ps,User u) throws SQLException{
        ps.setString(1,u.getFirstName());
        ps.setString(2,u.getLastName());
        ps.setString(3,u.getEmail());
        ps.setString(4,u.getPhone());
        ps.setString(5,u.getCity());
        ps.setString(6,u.getCountry());
        ps.setInt(7,u.getAge());
        ps.setString(8,u.getGender());
    }
    private List<User> mapResultSet(ResultSet rs) throws SQLException{
        List<User> users=new ArrayList<>();
        while(rs.next()){
            var u=new User();
            u.setId(rs.getLong("Id"));
            u.setFirstName(rs.getString("first_name"));
            u.setLastName(rs.getString("last_name"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setCity(rs.getString("city"));
            u.setCountry(rs.getString("country"));
            u.setGender(rs.getString("gender"));
            u.setAge(rs.getInt("age"));
            u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            users.add(u);


        }
        return users;

    }
}
