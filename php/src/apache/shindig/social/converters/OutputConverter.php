<?php
namespace apache\shindig\social\converters;
use apache\shindig\social\service\ResponseItem;
use apache\shindig\social\service\RestRequestItem;
use apache\shindig\common\SecurityToken;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Abstract class for the Output conversion of the RESTful API
 *
 */
abstract class OutputConverter {
  /**
   * @param ResponseItem $responseItem
   * @param RestRequestItem $requestItem
   */
  abstract function outputResponse(ResponseItem $responseItem, RestRequestItem $requestItem);

  /**
   * @param array $responses
   * @param SecurityToken $token
   */
  abstract function outputBatch(Array $responses, SecurityToken $token);
}