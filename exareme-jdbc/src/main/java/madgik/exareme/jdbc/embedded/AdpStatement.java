package madgik.exareme.jdbc.embedded;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;

/**
 *
 * @author dimitris
 */
public class AdpStatement implements Statement {

    private AdpConnection con;
    private boolean closeOnCompletion;
    private AdpResultSet resultSet;
    private Gson g;
    private HttpClient httpclient;
    private InputStreamReader result;
    private int timeout;

    public AdpStatement(AdpConnection connection) {
        this.con = connection;
        this.closeOnCompletion = false;
        this.resultSet = null;
        this.g = new Gson();
        httpclient = new DefaultHttpClient();
        timeout=0;
    }

    public AdpStatement(AdpConnection connection, int resultSetType,
            int resultSetConcurrency) {
        this.con = connection;
        this.closeOnCompletion = false;
        this.resultSet = null;
        this.g = new Gson();
        httpclient = new DefaultHttpClient();
    }

    @Override
    public AdpResultSet executeQuery(String sql) throws SQLException {

    	throw new UnsupportedOperationException("Not supported yet.");

    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
    	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws SQLException {
        if (this.result != null) {
            try {
                this.result.close();
            } catch (IOException ex) {
                throw new SQLException("Could not close connection");
            }
        }
        if (this.resultSet != null) {
            this.resultSet.close();
        }
        this.httpclient.getConnectionManager().shutdown();
        this.con = null;
        this.g = null;

    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0; // no limit
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaxRows() throws SQLException {
        return 0; // no limit
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return timeout;
        // need to check if it returns 0 as the default
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("query timeout must be >= 0");
        }
        //httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
        //        seconds * 1000);
        this.timeout=seconds;
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLFeatureNotSupportedException("Not supported.");
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean execute(String sql) throws SQLException {
    	throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (this.resultSet == null) {
            this.resultSet = new AdpResultSet(result, this);
        }
        return this.resultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        BufferedReader br = new BufferedReader(result);
        String line;
        try {
            line = br.readLine();
        } catch (java.io.EOFException ex) { // if no more results return -1
            return -1;
        } catch (IOException ex) { // other io errors
            throw new SQLException("Could not read update count");
        }
        Gson g = new Gson();
        try {
            return g.fromJson(line, Integer.class).intValue();
        } catch (Exception e) {
            return -1; // not update count
        }
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getFetchSize() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getResultSetType() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearBatch() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Connection getConnection() throws SQLException {
        return con;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.con == null;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void getResponse(AdpRequest q) throws SQLException {
        String json = g.toJson(q);
        try {
            HttpPost httppost = new HttpPost(con.getMetaData().getURL());
            StringEntity stringEntity = new StringEntity(json);
            stringEntity.setContentType("application/json");
            httppost.setEntity(stringEntity);
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity resEntity = response.getEntity();
            result = new InputStreamReader(resEntity.getContent());
        } catch (java.io.IOException e) {
            throw new SQLException("Connection to Database failed");
        }
    }



   
}
