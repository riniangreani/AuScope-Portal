package org.auscope.portal.server.web.controllers;

import java.net.URI;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpRequestBase;
import org.auscope.portal.core.configuration.ServiceConfiguration;
import org.auscope.portal.core.configuration.ServiceConfigurationItem;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.responses.wfs.WFSCountResponse;
import org.auscope.portal.core.services.responses.wfs.WFSResponse;
import org.auscope.portal.core.services.responses.wfs.WFSTransformedResponse;
import org.auscope.portal.core.test.PortalTestClass;
import org.auscope.portal.core.test.ResourceUtil;
import org.auscope.portal.server.web.service.MineralOccurrenceService;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author san218
 * @version $Id$
 */
public class TestEarthResourcesFilterController extends PortalTestClass {
    private EarthResourcesFilterController earthResourcesFilterController;
    private MineralOccurrenceService mineralOccurrenceService;

    @Before
    public void setUp() {
        this.mineralOccurrenceService = context.mock(MineralOccurrenceService.class);
        this.earthResourcesFilterController = new EarthResourcesFilterController(this.mineralOccurrenceService, 
                new ServiceConfiguration(new ArrayList<ServiceConfigurationItem>()));
    }

    private void testMAVResponse(ModelAndView mav, Boolean success, String gml) {
        ModelMap model = mav.getModelMap();

        if (success != null) {
            Assert.assertEquals(success.booleanValue(), model.get("success"));
        }

        if (gml != null) {
            ModelMap data = (ModelMap) model.get("data");

            Assert.assertNotNull(data);
            Assert.assertEquals(gml, data.get("gml"));
        }
    }

    private void testMAVResponseCount(ModelAndView mav, Boolean success, Integer count) {
        ModelMap model = mav.getModelMap();

        if (success != null) {
            Assert.assertEquals(success.booleanValue(), model.get("success"));
        }

        if (count != null) {
            Integer data = (Integer) model.get("data");
            Assert.assertNotNull(data);
            Assert.assertEquals(count, data);
        }
    }

    /**
     * Test doing a mine filter and getting all mines
     *
     * @throws Exception
     */
    @Test
    public void testDoMineFilterSpecificError() throws Exception {
        final String mineName = "testMine";
        final String serviceUrl = "http://testblah.com";
        final HttpRequestBase mockMethod = context.mock(HttpRequestBase.class);

        context.checking(new Expectations() {
            {
                allowing(mockMethod).getURI();
                will(returnValue(new URI(serviceUrl)));

                oneOf(mineralOccurrenceService).getMinesGml(serviceUrl, mineName, null, 0);
                will(throwException(new PortalServiceException(mockMethod)));
            }
        });

        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilter(serviceUrl, mineName, null, 0);

        //Ensure that we get a response that says failure
        testMAVResponse(modelAndView, new Boolean(false), null);
    }

    /**
     * Test doing a mine filter and getting all mines
     *
     * @throws Exception
     */
    @Test
    public void testDoMineFilterAnyError() throws Exception {
        final String serviceUrl = "http://testblah.com";
        final String expectedKML = "";
        final String mineName = "mineName";
        final HttpRequestBase mockMethod = context.mock(HttpRequestBase.class);
        final String xmlErrorResponse = ResourceUtil
                .loadResourceAsString("org/auscope/portal/core/test/responses/ows/OWSExceptionSample1.xml");

        context.checking(new Expectations() {
            {
                oneOf(mineralOccurrenceService).getMinesGml(serviceUrl, mineName, null, 0);
                will(returnValue(new WFSTransformedResponse(xmlErrorResponse, expectedKML, mockMethod)));
            }
        });

        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilter(serviceUrl, mineName, null, 0);

        //Ensure that we get a response that says failure
        testMAVResponse(modelAndView, new Boolean(false), null);
    }

    /**
     * Test doing a mine filter and getting all mines
     *
     * @throws Exception
     */
    @Test
    public void testDoMineFilterAllMines() throws Exception {
        final String serviceUrl = "http://localhost?";
        final String mineName = ""; //to get all mines
        final HttpRequestBase mockMethod = context.mock(HttpRequestBase.class);
        final String expectedGML = "<gml/>";

        context.checking(new Expectations() {
            {
                allowing(mockMethod).getURI();
                will(returnValue(new URI(serviceUrl)));
                oneOf(mineralOccurrenceService).getMinesGml(serviceUrl, mineName, null, 0);
                will(returnValue(new WFSResponse(expectedGML, mockMethod)));
            }
        });

        //call with updateCSWRecords dud url
        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilter(serviceUrl, mineName, null, 0);

        //Ensure that we get a valid response
        testMAVResponse(modelAndView, new Boolean(true), expectedGML);
    }

    /**
     * Test doing a mine filter and getting all mines
     *
     * @throws Exception
     */
    @Test
    public void testDoMineFilterSingleMine() throws Exception {
        final String serviceUrl = "http://localhost?";
        final String mineName = "mineName"; //to get all mines
        final HttpRequestBase mockMethod = context.mock(HttpRequestBase.class);
        final String expectedGML = "<gml/>";

        context.checking(new Expectations() {
            {
                allowing(mockMethod).getURI();
                will(returnValue(new URI(serviceUrl)));
                oneOf(mineralOccurrenceService).getMinesGml(serviceUrl, mineName, null, 0);
                will(returnValue(new WFSResponse(expectedGML, mockMethod)));
            }
        });

        //call with updateCSWRecords dud url
        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilter(serviceUrl, mineName, null, 0);

        //Ensure that we get a valid response
        testMAVResponse(modelAndView, new Boolean(true), expectedGML);
    }

    @Test
    public void testRequestFailure() throws Exception {
        final String serviceUrl = "http://localhost?";
        final String mineName = "mineName"; //to get all mines

        context.checking(new Expectations() {
            {
                oneOf(mineralOccurrenceService).getMinesGml(serviceUrl, mineName, null, 0);
                will(throwException(new PortalServiceException("")));
            }
        });

        //call with updateCSWRecords dud url
        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilter(serviceUrl, mineName, null, 0);

        //Ensure that we get a valid response
        testMAVResponse(modelAndView, new Boolean(false), null);
    }

    /**
     * Tests using the mine count service
     *
     * @throws Exception
     */
    @Test
    public void testMineCount() throws Exception {
        final String serviceUrl = "http://localhost?";
        final String mineName = "mineName"; //to get all mines
        final int maxFeatures = 21341;
        final int responseCount = 21;

        context.checking(new Expectations() {
            {
                oneOf(mineralOccurrenceService).getMinesCount(serviceUrl, mineName, null, maxFeatures);
                will(returnValue(new WFSCountResponse(responseCount)));
            }
        });

        ModelAndView modelAndView = this.earthResourcesFilterController.doMineFilterCount(serviceUrl, mineName, null,
                maxFeatures);
        testMAVResponseCount(modelAndView, new Boolean(true), responseCount);
    }

    /**
     * Tests using the mineral occurrence count service
     *
     * @throws Exception
     */
    @Test
    public void testMineralOccurrenceCount() throws Exception {
        final String serviceUrl = "http://localhost?";
        final int maxFeatures = 21341;
        final int responseCount = 21;
        final String commodityName = "cn";
        final String measureType = "mt";
        final String minOreAmount = "1";
        final String minOreAmountUOM = "2";
        final String minCommodityAmount = "3";
        final String minCommodityAmountUOM = "4";

        context.checking(new Expectations() {
            {
                oneOf(mineralOccurrenceService).getMineralOccurrenceCount(serviceUrl, commodityName, measureType,
                        minOreAmount, minOreAmountUOM, minCommodityAmount, minCommodityAmountUOM, maxFeatures, null);
                will(returnValue(new WFSCountResponse(responseCount)));
            }
        });

        ModelAndView modelAndView = this.earthResourcesFilterController.doMineralOccurrenceFilterCount(serviceUrl,
                commodityName, measureType, minOreAmount, minOreAmountUOM, minCommodityAmount, minCommodityAmountUOM,
                null, maxFeatures);
        testMAVResponseCount(modelAndView, new Boolean(true), responseCount);
    }

    /**
     * Tests using the mine activity count service
     *
     * @throws Exception
     */
    @Test
    public void testMineActivityCount() throws Exception {
        final String serviceUrl = "http://localhost?";
        final int maxFeatures = 21341;
        final int responseCount = 21;
        final String mineName = "mineName"; //to get all mines
        final String startDate = "2010-01-01";
        final String endDate = "2011-01-01";
        final String oreProcessed = "3";
        final String producedMaterial = "pm";
        final String cutOffGrade = "55";
        final String production = "prod";
        context.checking(new Expectations() {
            {
                oneOf(mineralOccurrenceService).getMiningActivityCount(serviceUrl, mineName, startDate, endDate,
                        oreProcessed, producedMaterial, cutOffGrade, production, maxFeatures, null);
                will(returnValue(new WFSCountResponse(responseCount)));
            }
        });

        ModelAndView modelAndView = this.earthResourcesFilterController.doMiningActivityFilterCount(serviceUrl,
                mineName, startDate, endDate, oreProcessed, producedMaterial, cutOffGrade, production, null,
                maxFeatures);
        testMAVResponseCount(modelAndView, new Boolean(true), responseCount);
    }
}
