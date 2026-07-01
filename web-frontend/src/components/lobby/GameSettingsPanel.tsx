import { useEffect, useRef, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Switch } from "../ui/switch";
import { Settings, Save, Loader2 } from "lucide-react";
import type { GameSettings } from "../../lib/types";

const clamp = (n: number, min: number, max: number) => Math.max(min, Math.min(max, n));

interface GameSettingsPanelProps {
  current: GameSettings;
  maxPlayers: number;
  isHost: boolean;
  onSave: (settings: GameSettings) => Promise<void>;
}

export function GameSettingsPanel({ current, maxPlayers, isHost, onSave }: GameSettingsPanelProps) {
  const [draft, setDraft] = useState<GameSettings>(current);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync draft when host updates settings remotely
  useEffect(() => {
    setDraft(current);
  }, [current]);

  const update = <K extends keyof GameSettings>(key: K, value: GameSettings[K]) => {
    if (!isHost) return;
    setDraft((prev) => ({ ...prev, [key]: value }));
  };

  const dirty = JSON.stringify(draft) !== JSON.stringify(current);
  const maxMafia = Math.max(1, Math.floor((maxPlayers - 1) / 2));

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    // Clamp everything to valid ranges only now, at save time, so typing
    // intermediate digits (e.g. "4" on the way to "40") doesn't get overwritten.
    const clamped: GameSettings = {
      ...draft,
      mafiaCount: clamp(draft.mafiaCount || 1, 1, maxMafia),
      sheriffInvestigationDelay: Math.max(0, draft.sheriffInvestigationDelay || 0),
      nightDurationSeconds: clamp(draft.nightDurationSeconds || 5, 5, 600),
      dayDurationSeconds: clamp(draft.dayDurationSeconds || 5, 5, 600),
      votingDurationSeconds: clamp(draft.votingDurationSeconds || 5, 5, 600),
    };
    try {
      await onSave(clamped);
    } catch (e) {
      setError((e as Error).message || "Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader className="py-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Settings className="w-4 h-4" />
          Game Settings
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Mafia count */}
        <div className="space-y-1">
          <Label htmlFor="mafiaCount" className="text-sm">
            Mafia members
          </Label>
          <IntegerInput
            id="mafiaCount"
            value={draft.mafiaCount}
            onChange={(v) => update("mafiaCount", v)}
            disabled={!isHost}
            className="w-24"
          />
          <p className="text-xs text-muted-foreground">Max {maxMafia} for {maxPlayers}-player lobbies</p>
        </div>

        {/* Role toggles */}
        <div className="space-y-2">
          <Label className="text-sm">Special Roles</Label>
          <RoleToggle
            label="Sheriff"
            description="Investigates one player each night"
            checked={draft.includeSheriff}
            onChange={(v) => update("includeSheriff", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Doctor"
            description="Heals one player each night"
            checked={draft.includeDoctor}
            onChange={(v) => update("includeDoctor", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Jester"
            description="Wins if voted out"
            checked={draft.includeJester}
            onChange={(v) => update("includeJester", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Mutilator"
            description="Mutes a player or revokes their vote"
            checked={draft.includeMutilator}
            onChange={(v) => update("includeMutilator", v)}
            disabled={!isHost}
          />
        </div>

        {/* Conditional: doctor self-save limit */}
        {draft.includeDoctor && (
          <div className="space-y-1">
            <Label htmlFor="doctorSelfSaveLimit" className="text-sm">
              Doctor self-save limit
            </Label>
            <Input
              id="doctorSelfSaveLimit"
              type="number"
              min={-1}
              value={draft.doctorSelfSaveLimit}
              onChange={(e) => update("doctorSelfSaveLimit", parseInt(e.target.value) || 0)}
              disabled={!isHost}
              className="w-24"
            />
            <p className="text-xs text-muted-foreground">-1 = unlimited</p>
          </div>
        )}

        {/* Conditional: sheriff investigation delay */}
        {draft.includeSheriff && (
          <div className="space-y-1">
            <Label htmlFor="sheriffInvestigationDelay" className="text-sm">
              Sheriff investigation delay (rounds)
            </Label>
            <IntegerInput
              id="sheriffInvestigationDelay"
              value={draft.sheriffInvestigationDelay}
              onChange={(v) => update("sheriffInvestigationDelay", v)}
              disabled={!isHost}
              className="w-24"
            />
            <p className="text-xs text-muted-foreground">0 = can investigate from round 1</p>
          </div>
        )}

        {/* Phase durations */}
        <div className="space-y-2">
          <Label className="text-sm">Phase Durations (seconds)</Label>
          <div className="grid grid-cols-3 gap-2">
            <DurationInput
              id="nightDurationSeconds"
              label="Night"
              value={draft.nightDurationSeconds}
              onChange={(v) => update("nightDurationSeconds", v)}
              disabled={!isHost}
            />
            <DurationInput
              id="dayDurationSeconds"
              label="Day"
              value={draft.dayDurationSeconds}
              onChange={(v) => update("dayDurationSeconds", v)}
              disabled={!isHost}
            />
            <DurationInput
              id="votingDurationSeconds"
              label="Voting"
              value={draft.votingDurationSeconds}
              onChange={(v) => update("votingDurationSeconds", v)}
              disabled={!isHost}
            />
          </div>
          <p className="text-xs text-muted-foreground">5–600 seconds per phase</p>
        </div>

        {error && <p className="text-sm text-red-500">{error}</p>}

        {isHost && dirty && (
          <Button onClick={handleSave} disabled={saving} className="w-full">
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-2" />
                Save Settings
              </>
            )}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

interface RoleToggleProps {
  label: string;
  description: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled: boolean;
}

function RoleToggle({ label, description, checked, onChange, disabled }: RoleToggleProps) {
  return (
    <div className="flex items-start justify-between gap-3 py-1">
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onChange} disabled={disabled} />
    </div>
  );
}

interface DurationInputProps {
  id: string;
  label: string;
  value: number;
  onChange: (v: number) => void;
  disabled: boolean;
}

function DurationInput({ id, label, value, onChange, disabled }: DurationInputProps) {
  return (
    <div className="space-y-1">
      <Label htmlFor={id} className="text-xs text-muted-foreground">
        {label}
      </Label>
      <IntegerInput id={id} value={value} onChange={onChange} disabled={disabled} />
    </div>
  );
}

interface IntegerInputProps {
  id: string;
  value: number;
  onChange: (v: number) => void;
  disabled: boolean;
  className?: string;
}

/**
 * Digit-only text input that never clamps mid-typing. Holds the raw string
 * locally so partial edits ("4" on the way to "40") aren't snapped to a min.
 * The parent is expected to clamp on save.
 */
function IntegerInput({ id, value, onChange, disabled, className }: IntegerInputProps) {
  const [text, setText] = useState<string>(String(value));
  const lastEmittedRef = useRef<number>(value);

  useEffect(() => {
    // Only re-sync when the prop changed for a reason other than our own emit
    // (e.g. remote settings update, form reset).
    if (value !== lastEmittedRef.current) {
      setText(String(value));
      lastEmittedRef.current = value;
    }
  }, [value]);

  return (
    <Input
      id={id}
      type="text"
      inputMode="numeric"
      value={text}
      className={className}
      onChange={(e) => {
        const raw = e.target.value;
        if (raw !== "" && !/^\d+$/.test(raw)) return;
        setText(raw);
        const n = raw === "" ? 0 : parseInt(raw, 10);
        lastEmittedRef.current = n;
        onChange(n);
      }}
      disabled={disabled}
    />
  );
}
