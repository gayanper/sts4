/*******************************************************************************
 * Copyright (c) 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
import { interfaces } from 'inversify';
import { createPreferenceProxy, PreferenceProxy, PreferenceService, PreferenceContribution, PreferenceSchema } from '@theia/core/lib/browser';

// tslint:disable:max-line-length

export const HIGHLIGHTS_PREF_NAME = 'boot-java.boot-hints.on';
export const XML_SUPPORT_PREF_NAME = 'boot-java.support-spring-xml-config.on';
export const CODELENS_PREF_NAME = 'boot-java.highlight-codelens.on';

export const BootConfigSchema: PreferenceSchema = {
    'type': 'object',
    'title': 'Spring Boot Configuration',
    properties: {
        'boot-java.boot-hints.on': {
            type: 'boolean',
            description: 'Enable/Disable Spring running Boot application live hints decorators in Java source code.',
            default: true
        },
        'boot-java.scan-java-test-sources.on': {
            type: 'boolean',
            description: 'Enable/Disable Java test sources files scanning',
            default: false
        },
        'boot-java.support-spring-xml-config.on': {
            type: 'boolean',
            description: 'Enable/Disable Support for Spring XML Config files',
            default: false
        },
        'boot-java.support-spring-xml-config.hyperlinks': {
            type: 'boolean',
            description: 'Enable/Disable Hyperlinks in Spring XML Config file editor',
            default: true
        },
        'boot-java.support-spring-xml-config.content-assist': {
            type: 'boolean',
            description: 'Enable/Disable Content Assist in Spring XML Config file editor',
            default: true
        },
        'boot-java.support-spring-xml-config.scan-folders-globs': {
            type: 'string',
            description: 'Scan Spring XML in folders',
            default: '**/src/main/**'
        },
        'boot-java.change-detection.on': {
            type: 'boolean',
            description: 'Enable/Disable detecting changes of running Spring Boot applications.',
            default: false
        },
        'boot-java.highlight-codelens.on': {
            type: 'boolean',
            default: true,
            description: 'Enable/Disable Spring running Boot application Code Lenses'
        },
        'spring-boot.ls.javahome': {
            type: 'string',
            default: null,
            description: "Java Home folder to start Spring Boot LS"
        },
        'spring-boot.ls.vmargs': {
            type: 'string',
            default: null,
            description: "Java VM arguments to start Spring Boot LS"
        }
    }
};

export interface BootConfiguration {
    'boot-java.boot-hints.on': boolean;
    'boot-java.scan-java-test-sources.on': boolean;
    'boot-java.support-spring-xml-config.on': boolean;
    'boot-java.support-spring-xml-config.hyperlinks': boolean;
    'boot-java.support-spring-xml-config.content-assist': boolean;
    'boot-java.support-spring-xml-config.scan-folders-globs': string;
    'boot-java.change-detection.on': boolean;
    'boot-java.highlight-codelens.on': boolean;
    'spring-boot.ls.javahome': string;
    'spring-boot.ls.vmargs': string;
}

export const BootPreferences = Symbol('BootPreferences');
export type BootPreferences = PreferenceProxy<BootConfiguration>;

export function createBootPreferences(preferences: PreferenceService): BootPreferences {
    return createPreferenceProxy(preferences, BootConfigSchema);
}

export function bindBootPreferences(bind: interfaces.Bind): void {
    bind(BootPreferences).toDynamicValue(ctx => {
        const preferences = ctx.container.get<PreferenceService>(PreferenceService);
        return createBootPreferences(preferences);
    });
    bind(PreferenceContribution).toConstantValue({ schema: BootConfigSchema });
}
