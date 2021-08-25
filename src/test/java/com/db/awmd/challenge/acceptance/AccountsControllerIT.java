package com.db.awmd.challenge.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerIT {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }
  
  @Test
  public void transfer() throws Exception {
	  Account accountFromTransfer = new Account("Id-111", new BigDecimal("100.00"));
	  Account accountToTransfer = new Account("Id-112", new BigDecimal("110.00"));
	  accountsService.createAccount(accountFromTransfer);
	  accountsService.createAccount(accountToTransfer);

	  mockMvc.perform(put("/v1/accounts/transfer")
	  	.contentType(MediaType.APPLICATION_JSON)
	  	.content("{\"accountFromId\":\"Id-111\",\"accountToId\":\"Id-112\",\"amountToTransfer\":60}"))
		.andExpect(status().isOk());
  }
  
  @Test
  public void transferNegativeAmount() throws Exception {
	  Account accountFromTransfer = new Account("Id-113", new BigDecimal("100.00"));
	  Account accountToTransfer = new Account("Id-114", new BigDecimal("110.00"));
	  accountsService.createAccount(accountFromTransfer);
	  accountsService.createAccount(accountToTransfer);

	  mockMvc.perform(put("/v1/accounts/transfer")
	  	.contentType(MediaType.APPLICATION_JSON)
	  	.content("{\"accountFromId\":\"Id-113\",\"accountToId\":\"Id-114\",\"amountToTransfer\":-5}"))
		.andExpect(status().isBadRequest());
  }
  
  @Test
  public void transferWithoutFounds() throws Exception {
	  Account accountFromTransfer = new Account("Id-115", new BigDecimal("100.00"));
	  Account accountToTransfer = new Account("Id-116", new BigDecimal("110.00"));
	  accountsService.createAccount(accountFromTransfer);
	  accountsService.createAccount(accountToTransfer);

	  mockMvc.perform(put("/v1/accounts/transfer")
	  	.contentType(MediaType.APPLICATION_JSON)
	  	.content("{\"accountFromId\":\"Id-115\",\"accountToId\":\"Id-116\",\"amountToTransfer\":110}"))
		.andExpect(status().isBadRequest());
  }
  
  @Test
  public void transferWhenAccountDoesNotExists() throws Exception {
	  Account accountFromTransfer = new Account("Id-117", new BigDecimal("100.00"));
	  accountsService.createAccount(accountFromTransfer);

	  mockMvc.perform(put("/v1/accounts/transfer")
	  	.contentType(MediaType.APPLICATION_JSON)
	  	.content("{\"accountFromId\":\"Id-117\",\"accountToId\":\"Id-118\",\"amountToTransfer\":110}"))
		.andExpect(status().isNotFound());
  }
  
  @Test
  public void transferSameOriginAndDestinationAccount() throws Exception {
	  Account accountFromTransfer = new Account("Id-118", new BigDecimal("100.00"));
	  accountsService.createAccount(accountFromTransfer);

	  mockMvc.perform(put("/v1/accounts/transfer")
	  	.contentType(MediaType.APPLICATION_JSON)
	  	.content("{\"accountFromId\":\"Id-118\",\"accountToId\":\"Id-118\",\"amountToTransfer\":60}"))
		.andExpect(status().isBadRequest());
  }
  
  @Test
  public void transferNoBody() throws Exception {
    mockMvc.perform(put("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }
}
