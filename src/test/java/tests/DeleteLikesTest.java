package tests;

import com.vk.api.sdk.actions.Likes;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.likes.responses.DeleteResponse;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class DeleteLikesTest {
    private VkApiClient vk;
    private Likes likes;
    private TransportClient transportClient;
    private UserActor actor;
    private ClientResponse positiveClientResponse, negativeClientResponse;
    private HashMap<String, String> headers;

    final Logger logger = Logger.getLogger(DeleteLikesTest.class);

    @BeforeMethod
    public void setup() {
        logger.info("Setup method start");
        //Создаем мок TransportClient: имитируем HTTP-запросы
        transportClient = Mockito.mock(TransportClient.class);

        //Создаем экземпляр VkApiClient с моком TransportClient
        vk = new VkApiClient(transportClient);

        //Создаем экземпляр Likes для работы с методами likes API
        likes = new Likes(vk);

        //Создаем мок актора
        actor = Mockito.mock(UserActor.class);
    }

    @DataProvider(name = "deleteLikesDataProvider")
    public Object[][] deleteLikesDataProvider() {
        logger.info("Data provider start");
        //Создаем мок ответа от API в виде JSON (из описания результата с сайта)
        String positiveJsonResponse = "{\"likes\": 5}";
        String negativeJsonResponse = "{\"error\": {\"error_code\": 100, \"error_msg\": \"One of the parameters specified was missing or invalid\"}}";

        //Заполняем map хэдером
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        //Создаем объекты ответов
        positiveClientResponse = new ClientResponse(200, positiveJsonResponse, headers);
        negativeClientResponse = new ClientResponse(422, negativeJsonResponse, headers);
        return new Object[][]{
                // positive case
                {positiveClientResponse, 200,positiveJsonResponse,5, null},
                // negative case with client error
                {negativeClientResponse, 422, negativeJsonResponse, null, "Internal API server error. Wrong status code: 422. Content: "+negativeJsonResponse},
        };
    }

    @Test(dataProvider = "deleteLikesDataProvider")
    public void testDeleteLike(ClientResponse mockResponse,int expectedStatusCode, String responseBody, Integer expectedLikes, String expectedErrorMessage) throws Exception {
        logger.info("DDT test start");
        // Делаем мок для метода post() TransportClient - при отправке определенных параметров возвращаем наш ответ
        when(transportClient.post(anyString(), anyString(), (org.apache.http.Header[]) any())).thenReturn(mockResponse);

        if (expectedErrorMessage == null) {
            try{
                // Вызов метода delete, если ожидается успешный результат
                DeleteResponse response = vk.likes().delete(actor, Type.POST, 12345).execute();
                // Проверяем, что количество лайков совпадает с ожидаемым
                assertEquals(response.getLikes().intValue(), expectedLikes.intValue());
                logger.info("Test passed");
            }catch (AssertionError e){
                logger.error("Test failed");
                logger.error(e.getMessage());
                throw e;
            }
        } else {
            // Проверяем, что выбрасывается ClientException с ожидаемым сообщением
            try {
                vk.likes().delete(actor, Type.POST, 12345).execute();
                logger.error("Test failed. Expected error were not occurred");
                fail("Expected ClientException to be thrown");
            } catch (ClientException e) {
                // Проверяем содержимое ошибки
                assertEquals(e.getMessage(), expectedErrorMessage);
                logger.info("Test passed");
            }
        }
    }
}
