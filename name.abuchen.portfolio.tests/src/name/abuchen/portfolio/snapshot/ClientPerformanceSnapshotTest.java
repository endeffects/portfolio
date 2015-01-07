package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Category;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class ClientPerformanceSnapshotTest
{
    private final Date startDate = Dates.date(2010, Calendar.DECEMBER, 31);
    private final Date endDate = Dates.date(2011, Calendar.DECEMBER, 31);

    @Test
    public void testDepositPlusInterest()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JUNE, 1), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);
        assertNotNull(snapshot);

        assertNotNull(snapshot.getStartClientSnapshot());
        assertEquals(startDate, snapshot.getStartClientSnapshot().getTime());

        assertNotNull(snapshot.getEndClientSnapshot());
        assertEquals(endDate, snapshot.getEndClientSnapshot().getTime());

        List<Category> categories = snapshot.getCategories();
        assertNotNull(categories);
        assertEquals(9, categories.size());

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testDepositPlusInterestFirstDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(105000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testDepositPlusInterestLastDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(0, result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testEarningsFromSecurity()
    {
        Client client = new Client();

        Security security = new Security();
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 10, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JANUARY, 31), CurrencyUnit.EUR, 50_00,
                        security, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
    }

    @Test
    public void testCapitalGains()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(10000, result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(110000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithBuyDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(10000 + (11000 - 9900), result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(121000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.SELL, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(1000 * 9 + (9900 - 10000), result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(11000 * 9, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriodWithFees()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.SELL, 1, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(1000 * 9 + (9900 - 10000) + 1, result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(1, result.get(CategoryType.FEES).getValuation());
    }

}