/*
 * This file is part of WaybackAuthLib - https://github.com/FlorianMichael/WaybackAuthLib
 * Copyright (C) 2023-2026 FlorianMichael/EnZaXD <git@florianmichael.de> and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianmichael.waybackauthlib;

public class InvalidRequestException extends Exception {

    public final String error;
    public final String errorMessage;
    public final String cause;

    public InvalidRequestException(String error) {
        super(error);

        this.error = error;
        this.errorMessage = null;
        this.cause = null;
    }

    public InvalidRequestException(String error, String errorMessage, String cause) {
        super(error + ": " + errorMessage + " (" + cause + ")");

        this.error = error;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

}
