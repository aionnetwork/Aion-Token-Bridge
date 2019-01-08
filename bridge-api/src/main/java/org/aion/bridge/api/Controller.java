/*
 *
 *   This code is licensed under the MIT License
 *
 *   Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */

package org.aion.bridge.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@CrossOrigin
@RestController
public class Controller implements ErrorController, InitializingBean {

    @Autowired
    private Model db;

    @Autowired
    private Environment env;

    private boolean isMaintenance = false;
    private static final String MAINTENANCE_PROP = "aion.bridge.maintenance";

    @Override
    public void afterPropertiesSet() throws Exception {
        if (env.containsProperty(MAINTENANCE_PROP)) {
            String prop = env.getProperty(MAINTENANCE_PROP);
            isMaintenance = prop.equalsIgnoreCase("true");
        }

        log.info("--------------------------------------------------");
        if (isMaintenance) {
            log.debug("Maintenance Mode = Enabled");
            log.debug("NOTE: UI will show a \"Under Maintenance\" message");
        } else {
            log.debug("Maintenance Mode = Disabled");
        }
        log.info("--------------------------------------------------");
    }

    public static final String ERROR_MAPPING = "/error";

    private Logger log = LoggerFactory.getLogger("bridge_api");

    @RequestMapping(value = "/maintenance", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<DtoMaintenance> maintenance() {
        DtoMaintenance response = new DtoMaintenance(isMaintenance);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
    }

    @RequestMapping(value = "/balance", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<DtoBridgeBalanceStatus> balance() throws JsonProcessingException {
        Optional<DtoBridgeBalanceStatus> response = db.getBalance();

        //noinspection OptionalIsPresent
        if (!response.isPresent())
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response.get());
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<DtoFinalizationStatus> status() throws JsonProcessingException {
        Optional<DtoFinalizationStatus> response = db.getFinalizationStatus();

        //noinspection OptionalIsPresent
        if (!response.isPresent())
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response.get());
    }

    @RequestMapping(value = "/transaction", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<DtoEthTransactionState> transaction(@RequestParam(required=true) String ethTxHash)
            throws JsonProcessingException {
        // validate input
        String hash = Utils.cleanHexString(ethTxHash);
        if (!Utils.isValidEthTxHash(hash)) {
            log.debug("[Error] /transfer: Bad hash: {}", ethTxHash);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Optional<DtoEthTransactionState> response = db.getEthTransactionState(hash);

        //noinspection OptionalIsPresent
        if (!response.isPresent())
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response.get());
    }

    @RequestMapping(value = "/batch", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<DtoTransactionAndFinalizationStatus> batch(@RequestParam(required=true) String ethTxHash)
            throws JsonProcessingException {
        // validate input
        String hash = Utils.cleanHexString(ethTxHash);
        if (!Utils.isValidEthTxHash(hash)) {
            log.debug("[Error] /batch: Bad hash: {}", ethTxHash);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Optional<DtoAionLatestBlock> status = db.getAionLatestBlock();
        Optional<DtoEthTransactionState> transaction = db.getEthTransactionState(hash);

        //noinspection OptionalIsPresent
        if (!transaction.isPresent())
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        DtoTransactionAndFinalizationStatus response =
                new DtoTransactionAndFinalizationStatus(transaction.get(), status.orElse(DtoAionLatestBlock.EMPTY));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
    }

    @RequestMapping(value = ERROR_MAPPING)
    public ResponseEntity<String> error() {
        log.trace("Error handler triggered");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public String getErrorPath() {
        return ERROR_MAPPING;
    }


}

