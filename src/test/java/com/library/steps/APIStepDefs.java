package com.library.steps;

import com.library.pages.BookPage;
import com.library.pages.LoginPage;
import com.library.utility.BrowserUtil;
import com.library.utility.ConfigurationReader;
import com.library.utility.DB_Util;
import com.library.utility.LibraryAPI_Util;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;
import java.util.LinkedHashMap;
import java.util.Map;


import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class APIStepDefs {

    RequestSpecification givenPart;
    Response response;
    ValidatableResponse thenPart;

    /**
     * US 01 RELATED STEPS
     */
    @Given("I logged Library api as a {string}")
    public void i_logged_library_api_as_a(String userType) {

        givenPart = given().log().uri()
                .header("x-library-token", LibraryAPI_Util.getToken(userType));
    }

    @Given("Accept header is {string}")
    public void accept_header_is(String contentType) {
        givenPart.accept(contentType);
    }

    @When("I send GET request to {string} endpoint")
    public void i_send_get_request_to_endpoint(String endpoint) {

        response = givenPart.when().get(ConfigurationReader.getProperty("library.baseUri") + endpoint).prettyPeek();
        thenPart = response.then();
    }

    @Then("status code should be {int}")
    public void status_code_should_be(Integer statusCode) {
        thenPart.statusCode(statusCode);
    }

    @Then("Response Content type is {string}")
    public void response_content_type_is(String contentType) {
        thenPart.contentType(contentType);
    }

    @Then("{string} field should not be null")
    public void field_should_not_be_null(String path) {
        thenPart.body(path, is(notNullValue()));
    }

    /**
     * US 05 RELATED STEPS
     *
     */

    @Given("I logged Library api with credentials {string} and {string}")
    public void i_logged_library_api_with_credentials_and(String email, String password) {
        token = LibraryAPI_Util.getToken(email,password);
        givenPart = given().log().uri()
                .header("x-library-token", token);
    }
    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String ReqContType) {
        givenPart.contentType(ReqContType);
    }
    @Given("I send token information as request body")
    public void i_send_token_information_as_request_body() {
        givenPart.formParams("token",token);
    }
    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {
        response = givenPart.when().post(ConfigurationReader.getProperty("library.baseUri") + endpoint).prettyPeek();
        thenPart = response.then();

    }
    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String key, String expValue) {
        String actualFieldValue = thenPart.extract().path(key);

        MatcherAssert.assertThat(actualFieldValue,equalTo(expValue));
    }

    /****************************** US04 - Rushana *****************************************/

    @Given("Request Content Type header is {string}")
    public void request_content_type_header_is(String contentType) {
        givenPart.contentType(contentType);
    }

    Map<String, Object> newUserMap;

    @Given("I create a random {string} as request body")
    public void i_create_a_random_as_request_body(String newUser) {
        Map<String, Object> requestBody = new LinkedHashMap<>();

        switch (newUser) {
            case "user":
                requestBody = LibraryAPI_Util.getRandomUserMap();
                break;

            case "book":
                requestBody = LibraryAPI_Util.getRandomBookMap();
                break;

            default:
                throw new RuntimeException("Unexpected value: " + newUser);
        }
        newUserMap = requestBody;
        givenPart.formParams(requestBody);
    }

    @When("I send POST request to {string} endpoint")
    public void i_send_post_request_to_endpoint(String endpoint) {
        response = givenPart
                .when()
                .post(ConfigurationReader.getProperty("library.baseUri") + endpoint);
        thenPart = response.then();
    }

    @Then("the field value for {string} path should be equal to {string}")
    public void the_field_value_for_path_should_be_equal_to(String messagePath, String expectedMessage) {
        thenPart.body(messagePath, is(expectedMessage));
    }

    /****************************** US04 @ui @db - Rushana *****************************************/

    @Then("created user information should match with Database")
    public void created_user_information_should_match_with_database() {
        String user_id = response.path("user_id");
        DB_Util.runQuery("select * from users where id =" + user_id);
        Map<String, Object> DB_user = DB_Util.getRowMap(1);

        Response apiData = given()
                .header("x-library-token", LibraryAPI_Util.getToken("librarian"))
                .pathParam("id", user_id)
                .when()
                .get(ConfigurationReader.getProperty("library.baseUri") + "/get_user_by_id/{id}");

        JsonPath jsonPath = apiData.jsonPath();

        Map<String, Object> APIUser = new LinkedHashMap<>();
        APIUser.put("id", jsonPath.getString("id"));
        APIUser.put("full_name", jsonPath.getString("full_name"));
        APIUser.put("email", jsonPath.getString("email"));
        APIUser.put("password", jsonPath.getString("password"));
        APIUser.put("user_group_id", jsonPath.getString("user_group_id"));
        APIUser.put("image", jsonPath.getString("image"));
        APIUser.put("extra_data", jsonPath.getString("extra_data"));
        APIUser.put("status", jsonPath.getString("status"));
        APIUser.put("is_admin", jsonPath.getString("is_admin"));
        APIUser.put("start_date", jsonPath.getString("start_date"));
        APIUser.put("end_date", jsonPath.getString("end_date"));
        APIUser.put("address", jsonPath.getString("address"));

        Assert.assertEquals(APIUser, DB_user);
    }

    LoginPage loginPage;

    @Then("created user should be able to login Library UI")
    public void created_user_should_be_able_to_login_library_ui() {
        loginPage = new LoginPage();
        String email = (String) newUserMap.get("email");
        String password = (String) newUserMap.get("password");

        loginPage.login(email, password);
        BrowserUtil.waitFor(2);
    }

    BookPage bookPage;
    @Then("created user name should appear in Dashboard Page")
    public void created_user_name_should_appear_in_dashboard_page() {
        bookPage = new BookPage();
        BrowserUtil.waitFor(2);

        String UIFullName = bookPage.accountHolderName.getText();
        String APIFullName = (String) newUserMap.get("full_name");

        Assert.assertEquals(APIFullName, UIFullName);
    }
}