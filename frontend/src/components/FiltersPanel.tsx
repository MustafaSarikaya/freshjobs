import type { ChangeEvent } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DATE_RANGE_PRESETS,
  FIELD_ORDER,
  FILTER_FIELD_DEFS,
  OPERATOR_LABELS,
} from "@/jobs/constants";
import type { FilterRule } from "@/jobs/filterExpression";
import type { JobsAction, JobsState } from "@/jobs/useJobsState";

type Props = {
  state: JobsState;
  dispatch: (action: JobsAction) => void;
  onApplied: () => void;
};

const VALUELESS_OPERATORS = new Set(["is_empty", "is_not_empty"]);
const DATE_INPUT_OPERATORS = new Set(["before", "after"]);

function fieldLabel(field: string) {
  return FILTER_FIELD_DEFS[field]?.label ?? field;
}

type DaysPresetPickerProps = {
  id: string;
  ariaLabel: string;
  value: string;
  onChange: (event: ChangeEvent<HTMLSelectElement>) => void;
};

function DaysPresetPicker({ id, ariaLabel, value, onChange }: DaysPresetPickerProps) {
  const isPreset = DATE_RANGE_PRESETS.some((p) => p.value === value);
  return (
    <select
      id={id}
      aria-label={ariaLabel}
      value={value}
      onChange={onChange}
      className="h-9 w-full min-w-0 rounded-md border border-input bg-background px-2 text-sm lg:min-w-40 lg:flex-1"
    >
      {!isPreset && value !== "" ? (
        <option value={value}>{`Last ${value} days`}</option>
      ) : null}
      {DATE_RANGE_PRESETS.map((preset) => (
        <option key={preset.value} value={preset.value}>
          {preset.label}
        </option>
      ))}
    </select>
  );
}

export function FiltersPanel({ state, dispatch, onApplied }: Props) {
  const { rules, renderable } = state;
  const disabled = !renderable;

  const handleAddRule = (event: ChangeEvent<HTMLSelectElement>) => {
    const field = event.target.value;
    if (!field) return;
    dispatch({ type: "ADD_RULE", field });
    event.target.value = "";
  };

  const handleOperatorChange = (rule: FilterRule) =>
    (event: ChangeEvent<HTMLSelectElement>) =>
      dispatch({
        type: "UPDATE_RULE_OPERATOR",
        ruleId: rule.id,
        operator: event.target.value,
      });

  const handleValueChange = (rule: FilterRule) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      dispatch({
        type: "UPDATE_RULE_VALUE",
        ruleId: rule.id,
        value: event.target.value,
      });

  const handleRemove = (rule: FilterRule) => () =>
    dispatch({ type: "REMOVE_RULE", ruleId: rule.id });

  const handleClear = () => dispatch({ type: "CLEAR_RULES" });

  const handleApply = () => {
    dispatch({ type: "COMMIT_FILTER" });
    onApplied();
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4">
      {disabled ? (
        <p
          role="alert"
          className="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950/50 dark:text-amber-200"
        >
          The current filter contains OR/NOT or nested groups and cannot be
          edited here. Clear it to build a new filter.
        </p>
      ) : null}

      <div
        data-testid="filters-panel-scroll"
        className="min-h-0 flex-1 space-y-3 overflow-y-auto pr-1"
      >
        {rules.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No filter rules yet. Pick a field below to add one.
          </p>
        ) : null}
        {rules.map((rule) => {
          const def = FILTER_FIELD_DEFS[rule.field];
          const operators = def?.operators ?? [rule.operator];
          const showValue = !VALUELESS_OPERATORS.has(rule.operator);
          const label = fieldLabel(rule.field);
          const isDateField = def?.type === "date";
          const isInLastDays = rule.operator === "in_last_days";
          const renderAsPresetPicker = isDateField && isInLastDays;
          const renderAsDateInput =
            isDateField && DATE_INPUT_OPERATORS.has(rule.operator);
          const safeValue = rule.value ?? "";
          return (
            <div
              key={rule.id}
              className="flex flex-col items-stretch gap-2 rounded-md border border-input p-2 lg:flex-row lg:flex-wrap lg:items-center"
            >
              <span className="text-sm font-medium lg:min-w-28">{label}</span>
              <label className="sr-only" htmlFor={`op-${rule.id}`}>
                {`Operator for ${label}`}
              </label>
              <select
                id={`op-${rule.id}`}
                aria-label={`Operator for ${label}`}
                value={rule.operator}
                onChange={handleOperatorChange(rule)}
                className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm lg:w-auto"
              >
                {operators.map((op) => (
                  <option key={op} value={op}>
                    {OPERATOR_LABELS[op] || op}
                  </option>
                ))}
              </select>
              {showValue ? (
                <>
                  <label className="sr-only" htmlFor={`val-${rule.id}`}>
                    {`Value for ${label}`}
                  </label>
                  {renderAsPresetPicker ? (
                    <DaysPresetPicker
                      id={`val-${rule.id}`}
                      ariaLabel={`Value for ${label}`}
                      value={safeValue}
                      onChange={handleValueChange(rule)}
                    />
                  ) : renderAsDateInput ? (
                    <Input
                      id={`val-${rule.id}`}
                      aria-label={`Value for ${label}`}
                      type="date"
                      value={safeValue}
                      onChange={handleValueChange(rule)}
                      className="h-9 min-w-0 lg:min-w-40 lg:flex-1"
                    />
                  ) : (
                    <Input
                      id={`val-${rule.id}`}
                      aria-label={`Value for ${label}`}
                      value={safeValue}
                      onChange={handleValueChange(rule)}
                      className="h-9 min-w-0 lg:min-w-40 lg:flex-1"
                    />
                  )}
                </>
              ) : null}
              <Button
                type="button"
                variant="outline"
                size="sm"
                aria-label={`Remove ${label} rule`}
                onClick={handleRemove(rule)}
                className="w-full lg:w-auto"
              >
                Remove
              </Button>
            </div>
          );
        })}
      </div>

      <div className="space-y-2">
        <label
          htmlFor="filters-add-rule"
          className="text-sm font-medium text-muted-foreground"
        >
          Add filter rule
        </label>
        <select
          id="filters-add-rule"
          aria-label="Add filter rule"
          defaultValue=""
          disabled={disabled}
          onChange={handleAddRule}
          className="h-9 w-full rounded-md border border-input bg-background px-2 text-sm"
        >
          <option value="" disabled>
            Pick a field…
          </option>
          {FIELD_ORDER.map((field) => (
            <option key={field} value={field}>
              {fieldLabel(field)}
            </option>
          ))}
        </select>
      </div>

      <div className="mt-auto grid grid-cols-1 gap-2 pt-4 lg:flex lg:items-center lg:justify-end">
        <Button
          type="button"
          variant="outline"
          onClick={handleClear}
          disabled={rules.length === 0 && state.expression === null}
          className="w-full lg:w-auto"
        >
          Clear filters
        </Button>
        <Button
          type="button"
          onClick={handleApply}
          disabled={disabled || rules.length === 0}
          className="w-full lg:w-auto"
        >
          Apply filters
        </Button>
      </div>
    </div>
  );
}
