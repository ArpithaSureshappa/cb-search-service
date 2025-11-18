package com.igot.cb.util;

/**
 * @author Mahesh RV
 */
public class Constants {


    public static final String KEYSPACE_SUNBIRD_COURSES = "sunbird_courses";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
    public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
    public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
    public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
    public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";
    public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String DOT = ".";
    public static final String OPEN_BRACE = "(";
    public static final String VALUES_WITH_BRACE = ") VALUES (";
    public static final String QUE_MARK = "?";
    public static final String COMMA = ",";
    public static final String CLOSING_BRACE = ")";
    public static final String RESPONSE = "response";
    public static final String SUCCESS = "success";
    public static final String FAILED = "Failed";
    public static final String ERROR_MESSAGE = "errmsg";
    public static final String INDEX_TYPE = "_doc";
    public static final String ERROR = "ERROR";
    public static final String KEYWORD = ".keyword";
    public static final String ASC = "asc";
    public static final String DOT_SEPARATOR = ".";
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String SUB = "sub";
    public static final String SSO_URL = "sso.url";
    public static final String SSO_REALM = "sso.realm";
    public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken.publickey.basepath";
    public static final String ID = "id";
    public static final String API_VERSION_1 = "1.0";
    public static final String X_AUTH_TOKEN = "x-authenticated-user-token";
    public static final String USER_ID_DOESNT_EXIST = "User Id doesn't exist! Please supply a valid auth token";
    public static final String SEARCH_OPERATION_LESS_THAN = "<";
    public static final String SEARCH_OPERATION_GREATER_THAN = ">";
    public static final String SEARCH_OPERATION_LESS_THAN_EQUALS = "<=";
    public static final String SEARCH_OPERATION_GREATER_THAN_EQUALS = ">=";
    public static final String MUST= "must";
    public static final String FILTER= "filter";
    public static final String MUST_NOT="must_not";
    public static final String SHOULD= "should";
    public static final String BOOL="bool";
    public static final String TERM="term";
    public static final String TERMS="terms";
    public static final String MATCH="match";
    public static final String RANGE="range";
    public static final String UNSUPPORTED_QUERY="Unsupported query type";
    public static final String UNSUPPORTED_RANGE= "Unsupported range condition";
    public static final String UPDATE = "UPDATE ";
    public static final String SET = " SET ";
    public static final String WHERE_ID = "where id";
    public static final String EQUAL_WITH_QUE_MARK = " = ? ";
    public static final String SEMICOLON = ";";
    public static final String USER = "user";
    public static final String UNKNOWN_IDENTIFIER = "Unknown identifier ";
    public static final String EXCEPTION_MSG_UPDATE = "Exception occurred while updating record to ";
    public static final String API_RECENT_SEARCH_CREATE = "api.recent.search.create";
    public static final String TABLE_USER_RECENT_SEARCH = "recent_searches_by_user";
    public static final String TABLE_SEARCH_INDEX_BY_USER = "search_index_by_user";
    public static final String TABLE_USER_RECENT_SEARCH_AUDIT = "recent_searches_history";
    public static final String API_RECENT_SEARCH_READ = "api.recent.search.read";
    public static final String API_TRENDING_SEARCH_CREATE = "api.trending.search.create";
    public static final String API_RECENT_SEARCH_DELETE = "api.recent.search.delete";
    public static final String ORDER_DESC = "desc";
    public static final String TABLE_TRENDING_SEARCH = "trending_searches_test";
    public static final String TRENDING_SEARCHES_INDEX_NAME = "trending_searches";
    public static final String API_TRENDING_SEARCH_READ = "api.trending.search.read";
    public static final String QUERY_ID = "query_id";
    public static final String SEARCH_COUNT = "search_count";
    public static final String LAST_SEARCHED = "last_searched";
    public static final String USERID = "user_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String SEARCH_CATEGORY = "search_category";
    public static final String SEARCH_CATEGORY_KEY = "searchCategory";
    public static final String PROCESSED_QUERY = "processedQuery";
    public static final String ACTUAL_QUERY = "actualQuery";
    public static final String TIMESTAMP = "timestamp";
    public static final String NLP_SEARCH_QUERY = "nlp_search_query";
    public static final String SEARCH_QUERY_KEY = "searchQuery";
    public static final String NLP_SEARCH_QUERY_KEY = "nlpSearchQuery";
    public static final String SEARCH_QUERY = "search_query";
    public static final String QUERY_NORM = "query_norm";
    public static final String OPERATION = "op";
    public static final Object REMOVE = "REMOVE";
    public static final String SEARCH_QUERIES = "searchQueries";
    public static final Object ADD = "ADD";


    private Constants() {
    }
}
