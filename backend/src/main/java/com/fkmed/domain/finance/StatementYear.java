package com.fkmed.domain.finance;

/**
 * A base year offered for a derived declaration (SPEC-0013 BR6/BR7): an income-tax (IR) statement
 * year (a year with payments) or a settlement-declaration year (a fully-paid year, Lei 12.007).
 */
public record StatementYear(int year) {}
