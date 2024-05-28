package tests;

import com.vk.api.sdk.actions.Likes;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.likes.responses.AddResponse;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.HashMap;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;


public class AddLikeTest {
    private VkApiClient vk;
    private Likes likes;
    private TransportClient transportClient;
    private UserActor actor;
    private ClientResponse positiveClientResponse, negativeClientResponse;
    private HashMap<String, String> headers;

    final Logger logger = Logger.getLogger(AddLikeTest.class);
    @BeforeMethod
    public void setup() {
        logger.info("Setup method start");
        //Создаем мок TransportClient: имитируем HTTP-запросы
        transportClient = Mockito.mock(TransportClient.class);

        //Создаем экземпляр VkApiClient с моком TransportClient
        vk = new VkApiClient(transportClient);

        //Создаем экземпляр Likes для работы с методами API лайков
        likes = new Likes(vk);

        //Создаем мок актора
        actor = Mockito.mock(UserActor.class);

        //Создаем мок ответа от API в виде JSON (из описания результата с сайта)
        String positiveJsonResponse = "{\"likes\": 5}";
        String negativeJsonResponse = "{\"error\": {\"error_code\": 15, \"error_msg\": \"Access denied\"}}";

        //Заполняем map хэдером
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        //создаем объекты ответов
        positiveClientResponse = new ClientResponse(200, positiveJsonResponse, headers);
        negativeClientResponse = new ClientResponse(403, negativeJsonResponse, headers);
    }

    @Test
    public void testAddLike() throws Exception {
        logger.info("Positive test start");
        //Делаем мок для метода post() TransportClient - при отправке определенных параметров возвращаем наш ответ
        when(transportClient.post(anyString(), anyString(), (org.apache.http.Header[]) any())).thenReturn(positiveClientResponse);

        //Выполняем метод likes.add
        AddResponse response = likes.add(actor, Type.POST, 12345).execute();

        try {
            //Проверяем, что количество лайков совпадает с ожидаемым
            assertEquals(response.getLikes(), Integer.valueOf(5));
            logger.info("Test passed");
        }catch (AssertionError e){
            logger.error("Test failed");
            logger.error(e.getMessage());
            throw e;
        }

    }

    @Test
    public void testNegativeAddLike() throws Exception {
        logger.info("Negative test start");
        //Делаем мок для метода post() TransportClient - при отправке определенных параметров возвращаем наш ответ
        when(transportClient.post(anyString(), anyString(), (org.apache.http.Header[]) any())).thenReturn(negativeClientResponse);

        // Проверяем, что при выполнении метода likes.add с ошибочными данными выбрасывается нужная ошибка
        try {
            vk.likes().add(actor, Type.POST ,12345).execute();
            logger.error("Test failed. Expected error were not occurred");
            fail("Expected ClientException to be thrown");
        } catch (ClientException e) {
            // Проверяем содержимое ошибки
            assertEquals(e.getMessage(), "Internal API server error. Wrong status code: 403. Content: "+negativeClientResponse.getContent());
            logger.info("Test passed");
        }
    }
}