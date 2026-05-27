package com.userapi.repository;

import com.userapi.config.DatabaseConfig;
import com.userapi.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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


//    fetching the data by it keyset
    public List<User> findAllKeyset(long afterId,int size,String country) throws SQLException{
        String sql= """
                SELECT *, COUNT(*) OVER() AS total_count FROM users WHERE id > ?
                """+ (country != null ? "AND country = ?" : "") + "ORDER BY id ASC LIMIT ?";

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            int idx=1;
            ps.setLong(idx++,afterId);
            if(country != null) ps.setString(idx++,country);
            ps.setInt(idx,size);
            return mapResultSet(ps.executeQuery());
        }

    }
//
//    load data with specific limit

    public List<User> findAllOffset(int page,int size,String country) throws SQLException{
        int safeSize=Math.min(size,100);
        int safeOffset=Math.max(page,0) * safeSize;


        String sql="SELECT * FROM users"
                + (country !=null  ? "WHERE country=?":"")
                + " ORDER BY id LIMIT ? OFFSET";

        try(Connection c= DatabaseConfig.getDataSource().getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            int idx=1;
            if(country != null) ps.setString(idx++,country);
            ps.setInt(idx++,safeSize);
            ps.setInt(idx,safeOffset);
            ps.setFetchSize(safeSize);
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

    private static final Set<String> ALLOWED_FIELDS= Set.of("name","email","phone");

//    optimised partial update
    public Optional<User> patch(long id, Map<String,Object> fields) throws SQLException{
        if(fields.isEmpty()) return findById(id);

        StringBuilder sql=new StringBuilder("UPDATE users SET");
        List<Object> values=new ArrayList<>();
//        fields.forEach((col,val)->{
//            sql.append(col).append("= ?, ");
//            values.add(val);
//
//
//        });
        for(Map.Entry<String,Object> entry:fields.entrySet()){
            String col=entry.getKey();

            if(ALLOWED_FIELDS.contains(col)){
                throw new IllegalArgumentException("Invalid fields : " + col);
            }

            sql.append(col).append(" = ?, ");
            values.add(entry.getValue());
        }
        sql.setLength(sql.length()-2);
        sql.append("WHERE id = ? RETURNING *");
        values.add(id);

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql.toString())){
            for(int i=0;i<values.size();i++) ps.setObject(i+1,values.get(i));

            List<User> list=mapResultSet(ps.executeQuery());
            return list.isEmpty() ? Optional.empty():Optional.of(list.getFirst());
        }
    }

    public boolean delete(long id) throws SQLException{
        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM users WHERE id=?")){
            ps.setLong(1,id);
            return ps.executeUpdate() > 0;
        }
    }

//    optimized search using PostgreSQL ts vector

    public List<User> search(String query,int limit) throws SQLException{
        String sql= """
                SELECT * FROM users WHERE to_tsvector('english',first_name || ' ' || last_name || ' ' || email)
                @@ plainto_tsquery('english',?) LIMIT ?
                """;

        try(Connection c=ds.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setString(1,query);
            ps.setInt(2,limit);
            return mapResultSet(ps.executeQuery());
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
