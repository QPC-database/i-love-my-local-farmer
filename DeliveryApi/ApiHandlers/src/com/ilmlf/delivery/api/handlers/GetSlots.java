/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ilmlf.delivery.api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import com.ilmlf.delivery.api.handlers.util.ApiUtil;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

/**
 * A Lambda handler for GetSlot API Call.
 */
public class GetSlots implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  /**
   * Constructor called by AWS Lambda.
   */
  public GetSlots() {
    this.slotService = new SlotService();
  }

  /**
   * Constructor for unit testing. Allows test code to inject mocked SlotService.
   *
   * @param slotService Injected SlotService object.
   */
  public GetSlots(SlotService slotService) {
    this.slotService = slotService;
  }

  /**
   * Handle get-slots GET via Api Gateway.
   * pathParameters expected: {farm-id=Integer}
   *
   * @return 200: success<br/>
   *        4xx: thrown if the farm-id is invalid or not found in the database<br/>
   *        5xx: if slots cannot be retrieved for the given farm
   */
  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    logger.info("Starting to handle GetSlots request");
    String returnVal = "";
    Integer httpStatus = 200;
    LocalDate availableSlotsBeginDate = LocalDate.now(ZoneId.of("UTC"));
    LocalDate availableSlotsEndDate = availableSlotsBeginDate.plus(Period.ofDays(14)); // Only retrieve next two weeks
    Integer farmId;
    ArrayList<Slot> slotArray;

    try {
      farmId = Integer.parseInt(input.getPathParameters().get("farm-id"));

    } catch (NumberFormatException exception) {
      throw new RuntimeException("Farm id must not be blank, and must be a valid integer");
    }

    try {
      slotArray = slotService.getSlots(farmId, availableSlotsBeginDate, availableSlotsEndDate);

      if (slotArray.isEmpty()) {
        httpStatus = 400;
        returnVal = "No slots found matching the farm id";
      } else {
        JSONArray slotJsonArray = new JSONArray(slotArray);
        returnVal = slotJsonArray.toString();
      }

    } catch (SQLException exception) {
      httpStatus = 500;
      returnVal = "Error encountered while retrieving slots from database";
      logger.error(exception.getMessage(), exception);
    }

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }
}
